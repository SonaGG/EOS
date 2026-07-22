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
     * Extracts a bundled file from `resources/natives` and returns its absolute path, or null when
     * the resource is not bundled. Used for dependencies EOS loads by path rather than by linking,
     * such as the Windows RTC XAudio redistributable.
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
        // Prefer a system-installed SDK, then fall back to the platform native bundled in resources.
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
        val stream = Native::class.java.classLoader.getResourceAsStream("natives/$name")
            ?: error("No bundled EOS native library '$name' found in resources/natives")

        val target: Path = tempDir().resolve(name)
        stream.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }

        val path = target.toAbsolutePath().toString()
        extracted[name] = path
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
fun Arena.allocCString(value: String?): MemorySegment =
    if (value == null) MemorySegment.NULL else allocateFrom(value)

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
    MemorySegment.copy(this, ValueLayout.JAVA_BYTE, offset, bytes, 0, maxBytes)
    val nul = bytes.indexOf(0.toByte())
    val len = if (nul < 0) maxBytes else nul
    return String(bytes, 0, len, Charsets.UTF_8)
}