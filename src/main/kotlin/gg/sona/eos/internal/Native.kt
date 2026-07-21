/*
 * Copyright 2024 sona
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

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Low-level FFM bindings for the EOS SDK shared library.
 *
 * The shared library is loaded once at class initialization. The C ABI is
 * little-endian and 64-bit; EOS uses the platform default calling convention.
 */
internal object Native {
    val linker: Linker = Linker.nativeLinker()
    private val lookup: SymbolLookup

    init {
        val candidates = listOf("EOSSDK", "EOSSDK-Win64-Shipping", "EOSSDK-Mac-Shipping", "EOSSDK-Linux-Shipping")
        var resolved: SymbolLookup? = null
        for (name in candidates) {
            try {
                System.loadLibrary(name)
                resolved = linker.defaultLookup()
                break
            } catch (_: UnsatisfiedLinkError) {
                // try next
            }
        }
        if (resolved == null) {
            extractBundledLibrary()
            resolved = SymbolLookup.loaderLookup()
        }
        lookup = resolved
    }

    fun downcall(name: String, descriptor: FunctionDescriptor): MethodHandle {
        val addr = lookup.find(name)
            .orElseThrow { UnsatisfiedLinkError("EOS symbol $name not found in shared library") }
        return linker.downcallHandle(addr, descriptor)
    }

    fun <T> invoke(name: String, descriptor: FunctionDescriptor, vararg args: Any?): T {
        @Suppress("UNCHECKED_CAST")
        return downcall(name, descriptor).invokeWithArguments(*args) as T
    }

    /** Invoke a void-returning downcall. */
    fun invokeVoid(name: String, args: List<Any?>, argLayouts: List<MemoryLayout>) {
        val layoutArray = argLayouts.toTypedArray()
        val descriptor = if (layoutArray.isEmpty()) {
            FunctionDescriptor.ofVoid()
        } else {
            FunctionDescriptor.ofVoid(*layoutArray)
        }
        downcall(name, descriptor).invokeWithArguments(*args.toTypedArray())
    }

    /** Invoke a value-returning downcall. */
    fun invoke(name: String, args: List<Any?>, argLayouts: List<MemoryLayout>, returnLayout: ValueLayout): Any? {
        val layoutArray = argLayouts.toTypedArray()
        val descriptor = FunctionDescriptor.of(returnLayout, *layoutArray)
        return downcall(name, descriptor).invokeWithArguments(*args.toTypedArray())
    }

    fun version(): String {
        val handle = downcall("EOS_GetVersion", FunctionDescriptor.of(ValueLayout.ADDRESS))
        val seg = handle.invokeExact() as MemorySegment
        return seg.reinterpret(Long.MAX_VALUE).getString(0)
    }

    private fun extractBundledLibrary() {
        val candidates = listOf(
            "EOSSDK-Win64-Shipping.dll",
            "libEOSSDK-Linux-Shipping.so",
            "libEOSSDK-Mac-Shipping.dylib",
        )
        var chosen: String? = null
        var stream: java.io.InputStream? = null
        for (name in candidates) {
            val s = Native::class.java.classLoader.getResourceAsStream("natives/$name")
            if (s != null) {
                chosen = name
                stream = s
                break
            }
        }
        if (chosen == null || stream == null) {
            error("No bundled EOS native library found in resources/natives")
        }
        val tmpDir = Files.createTempDirectory("eos-natives-")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runCatching {
                    Files.walk(tmpDir).sorted(Comparator.reverseOrder())
                        .forEach { Files.deleteIfExists(it) }
                }
            }
        )
        val target: Path = tmpDir.resolve(chosen)
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING)
        stream.close()
        System.load(target.toAbsolutePath().toString())
    }
}

/** Allocates a fresh [Arena] confined to the current thread for one call. */
@PublishedApi
internal inline fun <T> withCallArena(block: (Arena) -> T): T {
    return Arena.ofConfined().use(block)
}

/** Alias for [withCallArena] used by call sites that read better with this name. */
@PublishedApi
internal inline fun <T> withArena(block: (Arena) -> T): T = withCallArena(block)

/**
 * Run a block with the given struct value written into a fresh native arena.
 * The struct's [StructWriter.writeTo] is invoked once with the segment and
 * arena. The block must not retain the segment past its execution.
 */
@PublishedApi
internal inline fun <T> withStruct(value: StructWriter, block: (MemorySegment, Arena) -> T): T {
    return withCallArena { arena ->
        val segment = value.writeTo(arena)
        block(segment, arena)
    }
}

/** Allocates a null-terminated UTF-8 string in the arena, or [MemorySegment.NULL] if [value] is null. */
fun Arena.allocCString(value: String?): MemorySegment {
    if (value == null) return MemorySegment.NULL
    val bytes = value.toByteArray(Charsets.UTF_8)
    val seg = allocate(bytes.size + 1L, 1)
    seg.copyFrom(MemorySegment.ofArray(bytes))
    seg.set(ValueLayout.JAVA_BYTE, bytes.size.toLong(), 0)
    return seg
}

/** Allocates a UTF-8 string array in the arena; null entries become [MemorySegment.NULL]. */
fun Arena.allocCStringArray(values: List<String?>): MemorySegment {
    val arr = allocate(ValueLayout.ADDRESS, values.size.toLong())
    values.forEachIndexed { i, v ->
        arr.setAtIndex(ValueLayout.ADDRESS, i.toLong(), allocCString(v))
    }
    return arr
}

/** Allocates an array of opaque handle Longs (eos opaque pointers). */
fun Arena.allocHandleArray(values: List<Long>): MemorySegment {
    val arr = allocate(ValueLayout.JAVA_LONG, values.size.toLong())
    values.forEachIndexed { i, v ->
        arr.setAtIndex(ValueLayout.JAVA_LONG, i.toLong(), v)
    }
    return arr
}

/** Reads a null-terminated C string at the given offset, returning null if the pointer is null. */
fun MemorySegment.getCStringAt(offset: Long): String? {
    val ptr = get(ValueLayout.ADDRESS, offset)
    if (ptr.address() == 0L) return null
    return ptr.reinterpret(Long.MAX_VALUE).getString(0)
}

/** Reads a fixed-size byte array (e.g. SocketName[33]) as a UTF-8 string up to the first NUL. */
fun MemorySegment.getFixedCString(offset: Long, maxBytes: Int): String {
    val bytes = ByteArray(maxBytes)
    for (i in 0 until maxBytes) {
        val b = get(ValueLayout.JAVA_BYTE, offset + i)
        if (b == 0.toByte()) {
            return String(bytes, 0, i, Charsets.UTF_8)
        }
        bytes[i] = b
    }
    return String(bytes, Charsets.UTF_8)
}
