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

import gg.sona.eos.internal.Native
import gg.sona.eos.internal.withArena
import gg.sona.eos.internal.withStruct
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Main entry point for the Epic Online Services SDK.
 *
 * Call [initialize] once at startup, then [shutdown] once before the process
 * exits. The [EosPlatform] returned by [createPlatform] gives access to every
 * subsystem.
 */
public object Eos {

    private val initialized = AtomicBoolean(false)

    /** The version string of the loaded EOS SDK shared library (e.g. "1.19.1.2-12345"). */
    public val version: String by lazy { Native.version() }

    /** True if [initialize] has been called and [shutdown] has not yet been called. */
    public val isInitialized: Boolean
        get() = initialized.get()

    /**
     * Initialize the SDK. Must be called before any other API, and at most once
     * before a matching [shutdown] call.
     */
    public fun initialize(options: EosInitializeOptions): EosResult {
        if (initialized.get()) return EosResult.AlreadyConfigured
        val result = withStruct(options) { segment, _ ->
            val fn = Native.downcall(
                "EOS_Initialize",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
            )
            EosResult.fromValue(fn.invokeExact(segment) as Int)
        }
        if (result == EosResult.Success) initialized.set(true)
        return result
    }

    /**
     * Tear down the SDK. Must be called at most once and only after a successful
     * [initialize]. After this call returns, no other EOS API may be invoked.
     */
    public fun shutdown(): EosResult {
        if (!initialized.get()) return EosResult.NotConfigured
        val fn = Native.downcall("EOS_Shutdown", FunctionDescriptor.of(ValueLayout.JAVA_INT))
        val result = EosResult.fromValue(fn.invokeExact() as Int)
        if (result == EosResult.Success || result == EosResult.NotConfigured) initialized.set(false)
        return result
    }

    /**
     * Create a platform instance. The returned instance must be released by
     * calling [EosPlatform.close] (or with a `use { }` block).
     */
    public fun createPlatform(options: EosPlatformOptions): EosPlatform {
        if (!initialized.get()) error("EOS SDK is not initialized; call Eos.initialize first")
        return withStruct(options) { segment, _ ->
            val fn = Native.downcall(
                "EOS_Platform_Create",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
            )
            val handle = fn.invokeExact(segment) as Long

            // EOS_Platform_Create signals failure by returning null rather than a result code.
            // Wrapping that produces a platform whose every call logs "one or more parameters are
            // null" forever, with nothing pointing at the actual cause.
            if (handle == 0L) {
                error(
                    "EOS_Platform_Create returned null. Check the product, sandbox and deployment " +
                        "ids, the client credentials, and - when RTC is enabled on Windows - that " +
                        "xaudio2_9redist.dll could be located."
                )
            }

            EosPlatform(handle)
        }
    }

    /**
     * Convert a hex byte string into raw bytes. The output is at most
     * [expectedLength] bytes; the result is truncated to that length.
     */
    public fun byteArrayToString(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): String {
        require(offset >= 0 && length >= 0 && offset + length <= bytes.size) { "byte range out of bounds" }
        if (length == 0) return ""
        return withArena { arena ->
            val inBuf = arena.allocate(length.toLong())
            inBuf.copyFrom(MemorySegment.ofArray(bytes).asSlice(offset.toLong(), length.toLong()))
            val sizePtr = arena.allocate(ValueLayout.JAVA_INT)
            val fn = Native.downcall(
                "EOS_ByteArray_ToString",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                )
            )
            // First call: get required size
            fn.invokeExact(inBuf, length, MemorySegment.NULL, sizePtr) as Int
            val size = sizePtr.get(ValueLayout.JAVA_INT, 0)
            val outBuf = arena.allocate(size.toLong())
            sizePtr.set(ValueLayout.JAVA_INT, 0, size)
            val result = fn.invokeExact(inBuf, length, outBuf, sizePtr) as Int
            check(result == 0) { "EOS_ByteArray_ToString failed: ${EosResult.fromValue(result)}" }
            outBuf.getString(0)
        }
    }
}
