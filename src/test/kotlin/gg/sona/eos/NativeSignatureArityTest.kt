/*
 * Copyright 2026 Sona Softworks LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gg.sona.eos

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Cross-checks every native callsite in this binding against the real C
 * signature in the EOS SDK headers that ship in this repository.
 *
 * Why this exists: this binding is hand-written FFM/Panama glue. A callsite
 * that passes the wrong *number* of arguments still compiles, still links,
 * and still runs - the native function simply reads whatever garbage happens
 * to be in the missing argument's register. There is no diagnostic.
 *
 * That is not hypothetical. Every async callsite in this binding used to drop
 * the trailing `CompletionDelegate` parameter and pass the callback stub as
 * `ClientData` instead:
 *
 *     EOS_Connect_Login(Handle, Options, ClientData, CompletionDelegate)  // 4 params
 *     listOf(handle(), seg, stub.segment)                                 // 3 passed
 *
 * The SDK then stored an uninitialized register value as the completion
 * delegate and *called it* when the operation completed inside
 * `EOS_Platform_Tick`, jumping into non-executable memory. The result was a
 * fatal `EXCEPTION_ACCESS_VIOLATION` (a DEP violation at a heap address)
 * inside `tick()` - arbitrarily far from the actual mistake, with nothing in
 * the crash pointing back at the offending call.
 *
 * An arity check is the cheapest possible guard against that entire class of
 * bug, so it runs over the whole codebase rather than a hand-picked list.
 */
class NativeSignatureArityTest {

    private val repoRoot: File = findRepoRoot()
    private val headerDir = File(repoRoot, "EOS_SDK/SDK/Include")
    private val sourceDir = File(repoRoot, "src/main/kotlin")

    @Test
    fun `every native callsite passes the argument count its C signature declares`() {
        val headerArity = parseHeaderArities()
        assertTrue(
            headerArity.size > 100,
            "expected to parse a few hundred EOS function signatures, got ${headerArity.size} " +
                "- has the header layout or EOS_DECLARE_FUNC macro changed?",
        )

        val problems = mutableListOf<String>()
        var checked = 0

        for (callsite in parseCallsites()) {
            val expected = headerArity[callsite.function]
            if (expected == null) {
                problems += "${callsite.where}: calls \"${callsite.function}\", " +
                    "which does not exist in the EOS SDK headers"
                continue
            }
            checked++
            if (callsite.paramCount != expected) {
                problems += "${callsite.where}: \"${callsite.function}\" takes $expected " +
                    "parameter(s), but the callsite passes ${callsite.paramCount}"
            }
        }

        assertTrue(checked > 200, "expected to check 200+ callsites, only checked $checked")
        if (problems.isNotEmpty()) {
            fail("Native signature mismatches (${problems.size}):\n" + problems.joinToString("\n"))
        }
    }

    @Test
    fun `argument list and layout list agree at every callsite`() {
        val problems = parseCallsites()
            .filter { it.argCount != null && it.argCount != it.paramCount }
            .map {
                "${it.where}: \"${it.function}\" passes ${it.argCount} argument(s) " +
                    "but declares ${it.paramCount} layout(s)"
            }
        if (problems.isNotEmpty()) {
            fail("Argument/layout count mismatches (${problems.size}):\n" + problems.joinToString("\n"))
        }
    }

    /**
     * Async EOS functions all end in `(..., void* ClientData, <Callback> CompletionDelegate)`.
     * Passing a callback stub in the ClientData slot is the specific mistake that caused the
     * historical crash, and it leaves a recognisable shape: a `.segment` argument sitting one
     * position before the end of the list.
     */
    @Test
    fun `callback stubs are never passed in the ClientData position`() {
        val problems = parseCallsites().mapNotNull { callsite ->
            val args = callsite.args ?: return@mapNotNull null
            if (args.size < 2) return@mapNotNull null
            val clientDataSlot = args[args.size - 2]
            if (clientDataSlot.endsWith(".segment")) {
                "${callsite.where}: \"${callsite.function}\" passes `$clientDataSlot` as ClientData; " +
                    "the callback stub belongs in the final CompletionDelegate slot"
            } else {
                null
            }
        }
        if (problems.isNotEmpty()) {
            fail("Callback stub in ClientData position (${problems.size}):\n" + problems.joinToString("\n"))
        }
    }

    // ---------------------------------------------------------------- parsing

    private data class Callsite(
        val function: String,
        val paramCount: Int,
        val argCount: Int?,
        val args: List<String>?,
        val where: String,
    )

    /** Maps `EOS_Xxx` to the number of parameters its `EOS_DECLARE_FUNC` signature declares. */
    private fun parseHeaderArities(): Map<String, Int> {
        val arities = mutableMapOf<String, Int>()
        val declare = Regex(
            """EOS_DECLARE_FUNC\s*\([^)]*\)\s*(EOS_[A-Za-z0-9_]+)\s*\(""",
            RegexOption.DOT_MATCHES_ALL,
        )
        headerDir.walkTopDown().filter { it.extension == "h" }.forEach { header ->
            val text = header.readText()
            for (match in declare.findAll(text)) {
                val name = match.groupValues[1]
                val params = readBalanced(text, match.range.last) ?: continue
                arities[name] = splitTopLevel(params)
                    .count { it.isNotBlank() && it.trim() != "void" }
            }
        }
        return arities
    }

    /** Every `Native.invoke` / `Native.invokeVoid` / `Native.downcall` callsite in the binding. */
    private fun parseCallsites(): List<Callsite> {
        val callsites = mutableListOf<Callsite>()
        val entry = Regex("""Native\.(invokeVoid|invoke|downcall)\s*\(""")

        sourceDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            val relative = file.relativeTo(repoRoot).invariantSeparatorsPath

            for (match in entry.findAll(text)) {
                val kind = match.groupValues[1]
                val body = readBalanced(text, match.range.last) ?: continue
                val parts = splitTopLevel(body).map { it.trim() }
                if (parts.isEmpty()) continue

                val name = Regex("^\"(EOS_[A-Za-z0-9_]+)\"$")
                    .find(parts[0])?.groupValues?.get(1) ?: continue
                val line = text.take(match.range.first).count { it == '\n' } + 1
                val where = "$relative:$line"

                when (kind) {
                    "invokeVoid", "invoke" -> {
                        // invoke*(name, listOf(args), listOf(layouts) [, returnLayout])
                        if (parts.size < 3) continue
                        val args = parts[1].listOfContents() ?: continue
                        val layouts = parts[2].listOfContents() ?: continue
                        callsites += Callsite(name, layouts.size, args.size, args, where)
                    }
                    "downcall" -> {
                        // downcall(name, FunctionDescriptor.of(ret, layouts...) | .ofVoid(layouts...))
                        if (parts.size < 2) continue
                        val descriptor = parts[1]
                        val void = descriptor.contains("FunctionDescriptor.ofVoid")
                        val inner = readBalanced(descriptor, descriptor.indexOf('(')) ?: continue
                        val entries = splitTopLevel(inner).filter { it.isNotBlank() }
                        val count = if (void) entries.size else (entries.size - 1)
                        callsites += Callsite(name, count.coerceAtLeast(0), null, null, where)
                    }
                }
            }
        }
        return callsites
    }

    /** Contents of a `listOf(...)` expression, or null if this is not one. */
    private fun String.listOfContents(): List<String>? {
        if (!startsWith("listOf(")) return null
        val inner = readBalanced(this, indexOf('(')) ?: return null
        return splitTopLevel(inner).map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Text between the paren at [openIndex] and its match, honouring nesting and string literals. */
    private fun readBalanced(text: String, openIndex: Int): String? {
        var i = openIndex
        while (i < text.length && text[i] != '(') i++
        if (i >= text.length) return null
        val start = i + 1
        var depth = 0
        var inString = false
        while (i < text.length) {
            val c = text[i]
            when {
                inString && c == '\\' -> i++
                c == '"' -> inString = !inString
                inString -> {}
                c == '(' -> depth++
                c == ')' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i)
                }
            }
            i++
        }
        return null
    }

    /** Splits on commas that are not nested inside parens, angle brackets, or strings. */
    private fun splitTopLevel(text: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        var inString = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inString && c == '\\' -> {
                    current.append(c)
                    if (i + 1 < text.length) current.append(text[++i])
                }
                c == '"' -> { inString = !inString; current.append(c) }
                inString -> current.append(c)
                c == '(' || c == '<' -> { depth++; current.append(c) }
                c == ')' || c == '>' -> { depth--; current.append(c) }
                c == ',' && depth == 0 -> { parts += current.toString(); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        if (current.isNotBlank()) parts += current.toString()
        return parts
    }

    private fun findRepoRoot(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (!File(dir, "EOS_SDK/SDK/Include").isDirectory) {
            dir = dir.parentFile ?: error("could not locate the repository root from ${System.getProperty("user.dir")}")
        }
        return dir
    }
}
