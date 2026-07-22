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

import gg.sona.eos.EosNatives
import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpResponse
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
    private val resolved = ConcurrentHashMap<String, String>()

    @Volatile
    private var cacheDir: Path? = null

    private val httpClient: HttpClient by lazy {
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

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
     * Downloads (once) and returns the absolute path to a native support file that EOS loads by
     * path rather than by linking, such as the Windows RTC XAudio redistributable. The file is
     * fetched from the configured [EosNatives.baseUrl] and cached on disk; subsequent calls reuse
     * the cached copy.
     *
     * @throws IllegalStateException if no base URL is configured or the download fails.
     */
    fun nativeFilePath(name: String): String = downloadAndCache(name).toAbsolutePath().toString()

    private fun loadNativeLibrary(): SymbolLookup {
        // Prefer a system-installed SDK, then fall back to downloading the platform native.
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
        System.load(downloadAndCache(nativeLibraryName()).toAbsolutePath().toString())
        return SymbolLookup.loaderLookup()
    }

    /**
     * Returns the on-disk path to [name], downloading it from [EosNatives.baseUrl] into the cache
     * directory if it is not already cached. Downloads are written to a temporary file and moved
     * into place atomically so a partial download can never be mistaken for a complete one.
     */
    private fun downloadAndCache(name: String): Path {
        resolved[name]?.let { return Path.of(it) }

        synchronized(resolved) {
            resolved[name]?.let { return Path.of(it) }

            val target = cacheDir().resolve(name)
            if (Files.isRegularFile(target) && Files.size(target) > 0) {
                resolved[name] = target.toString()
                return target
            }

            val base = EosNatives.baseUrl?.trim()?.takeIf { it.isNotEmpty() } ?: error(
                "No EOS native binary base URL is configured, so '$name' cannot be located. " +
                        "This library does not distribute the EOS SDK binaries (they are owned by " +
                        "Epic Games and covered by Epic's own license). Set EosNatives.baseUrl (or the " +
                        "eos.natives.baseUrl system property / EOS_NATIVES_BASE_URL environment variable) " +
                        "to a URL you are licensed to fetch the binaries from, or install the EOS SDK " +
                        "system-wide."
            )

            val url = base.trimEnd('/') + "/" + name
            val tmp = Files.createTempFile(target.parent, "$name.", ".part")
            try {
                val response = httpClient.send(
                    java.net.http.HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofFile(tmp)
                )
                if (response.statusCode() !in 200..299) {
                    error("Failed to download EOS native '$name' from $url: HTTP ${response.statusCode()}")
                }
                if (Files.size(tmp) == 0L) {
                    error("Downloaded EOS native '$name' from $url is empty")
                }
                try {
                    Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (e: Exception) {
                runCatching { Files.deleteIfExists(tmp) }
                if (e is IllegalStateException) throw e
                throw IllegalStateException("Failed to download EOS native '$name' from $url", e)
            } finally {
                runCatching { Files.deleteIfExists(tmp) }
            }

            resolved[name] = target.toString()
            return target
        }
    }

    /** The native library filename for the current OS. */
    private fun nativeLibraryName(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> "EOSSDK-Win64-Shipping.dll"
            os.contains("mac") || os.contains("darwin") -> "libEOSSDK-Mac-Shipping.dylib"
            else -> "libEOSSDK-Linux-Shipping.so"
        }
    }

    private fun cacheDir(): Path {
        cacheDir?.let { return it }

        synchronized(this) {
            cacheDir?.let { return it }

            val dir = EosNatives.cacheDir
                ?: System.getProperty("user.home")
                    ?.let { Path.of(it, ".cache", "eos-natives") }
                ?: Files.createTempDirectory("eos-natives-")

            Files.createDirectories(dir)
            cacheDir = dir
            return dir
        }
    }
}

