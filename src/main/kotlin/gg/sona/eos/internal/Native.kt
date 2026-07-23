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
package gg.sona.eos.internal

import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * Low-level FFM bindings for the EOS SDK shared library.
 *
 * The shared library is loaded once at class initialization. Linked downcall
 * handles are cached per (symbol, descriptor): linking a handle is the
 * expensive part of a native call, so each symbol is resolved and linked at
 * most once and every subsequent call is a plain [MethodHandle] invoke.
 *
 * The C ABI is little-endian and 64-bit; EOS uses the platform default calling
 * convention.
 *
 * @author Luna
 */
internal object Native {
    val linker: Linker = Linker.nativeLinker()
    private val lookup: SymbolLookup

    // Declared above the init block on purpose: object properties initialize in declaration order,
    // and the block below loads the library through these fields.
    private val handles = ConcurrentHashMap<HandleKey, MethodHandle>()
    private val extracted = ConcurrentHashMap<String, String>()

    @Volatile
    private var tempDir: Path? = null

    init {
        lookup = loadNativeLibrary()
    }

    private data class HandleKey(val symbol: String, val descriptor: FunctionDescriptor)

    /**
     * Returns a cached downcall handle for [name] with the given [descriptor].
     * Symbol resolution and linking happen once per distinct (symbol, descriptor);
     * repeat calls hit the cache.
     */
    fun downcall(name: String, descriptor: FunctionDescriptor): MethodHandle =
        handles.computeIfAbsent(HandleKey(name, descriptor)) { key ->
            val addr = lookup.find(key.symbol)
                .orElseThrow { UnsatisfiedLinkError("EOS symbol ${key.symbol} not found in shared library") }
            linker.downcallHandle(addr, key.descriptor)
        }

    fun <T> invoke(name: String, descriptor: FunctionDescriptor, vararg args: Any?): T {
        @Suppress("UNCHECKED_CAST")
        return downcall(name, descriptor).invokeWithArguments(*args) as T
    }

    /** Invoke a void-returning downcall. */
    fun invokeVoid(name: String, args: List<Any?>, argLayouts: List<MemoryLayout>) {
        val descriptor = if (argLayouts.isEmpty()) {
            FunctionDescriptor.ofVoid()
        } else {
            FunctionDescriptor.ofVoid(*argLayouts.toTypedArray())
        }
        downcall(name, descriptor).invokeWithArguments(args)
    }

    /** Invoke a value-returning downcall. */
    fun invoke(name: String, args: List<Any?>, argLayouts: List<MemoryLayout>, returnLayout: ValueLayout): Any? {
        val descriptor = FunctionDescriptor.of(returnLayout, *argLayouts.toTypedArray())
        return downcall(name, descriptor).invokeWithArguments(args)
    }

    fun version(): String {
        val handle = downcall("EOS_GetVersion", FunctionDescriptor.of(ValueLayout.ADDRESS))
        val seg = handle.invokeExact() as MemorySegment
        return seg.reinterpret(Long.MAX_VALUE).getString(0)
    }

    /**
     * Extracts a file from `natives/[name]` on the classpath and returns its absolute path, or null
     * when the resource isn't present. This library does not bundle the EOS SDK binaries itself
     * (they are owned by Epic Games); the application embedding this library is expected to ship
     * them as a classpath resource under `natives/`, e.g. `src/main/resources/natives/` in the app's
     * own build. Used for dependencies EOS loads by path rather than by linking, such as the Windows
     * RTC XAudio redistributable.
     */
    fun extractBundledFile(name: String): String? {
        extracted[name]?.let { return it }

        synchronized(extracted) {
            extracted[name]?.let { return it }

            val stream = Native::class.java.classLoader.getResourceAsStream("natives/$name")
                ?: return null

            val target = tempDir().resolve(name)
            stream.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }

            val path = target.toAbsolutePath().toString()
            extracted[name] = path
            return path
        }
    }

    private fun loadNativeLibrary(): SymbolLookup {
        // Prefer a system-installed SDK, then fall back to the platform native bundled by the
        // embedding application's resources.
        val installed = listOf("EOSSDK", "EOSSDK-Win64-Shipping", "EOSSDK-Mac-Shipping", "EOSSDK-Linux-Shipping")
        for (name in installed) {
            try {
                System.loadLibrary(name)
                // loaderLookup (not defaultLookup) is the one that sees libraries loaded via
                // System.loadLibrary/System.load; defaultLookup only covers the linker's system set.
                return SymbolLookup.loaderLookup()
            } catch (_: UnsatisfiedLinkError) {
                // try next
            }
        }
        extractBundledLibrary()
        return SymbolLookup.loaderLookup()
    }

    private fun extractBundledLibrary() {
        val name = bundledLibraryName()
        val path = extractBundledFile(name) ?: error(
            "No EOS SDK native library '$name' found on the classpath under 'natives/$name', and no " +
                    "system-installed EOSSDK was found either. This library does not distribute the EOS " +
                    "SDK binaries (they are owned by Epic Games and covered by Epic's own license). The " +
                    "application embedding this library must supply '$name' as a classpath resource at " +
                    "'natives/$name' (for example, under 'src/main/resources/natives/' in the app's own " +
                    "build), or have the EOS SDK installed system-wide."
        )
        System.load(path)
    }

    /** The bundled native library filename for the current OS. */
    private fun bundledLibraryName(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> "EOSSDK-Win64-Shipping.dll"
            os.contains("mac") || os.contains("darwin") -> "libEOSSDK-Mac-Shipping.dylib"
            else -> "libEOSSDK-Linux-Shipping.so"
        }
    }

    private fun tempDir(): Path {
        tempDir?.let { return it }

        synchronized(this) {
            tempDir?.let { return it }

            val dir = Files.createTempDirectory("eos-natives-")
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    runCatching {
                        Files.walk(dir).sorted(Comparator.reverseOrder())
                            .forEach { Files.deleteIfExists(it) }
                    }
                }
            )
            tempDir = dir
            return dir
        }
    }
}