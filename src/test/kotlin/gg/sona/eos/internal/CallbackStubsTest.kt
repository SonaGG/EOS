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

import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the upcall exception guard in [CallbackStubs].
 *
 * Background: an exception thrown while a registered callback is running is
 * being invoked *from native code* (an "upcall") is undefined behavior per
 * the Java FFM API contract. In practice it does not fail cleanly at the
 * throw site - it corrupts the downcall/upcall stub machinery and crashes
 * the JVM with an access violation on a later, unrelated native call. This
 * was the root cause of a real crash: `EOS_Connect_Login`/`EOS_Auth_Login`
 * succeeding, then an unrelated automatic internal SDK auth retry failing,
 * whose failure callback threw (because failure payloads have different/
 * absent fields than success payloads), and the JVM crashing on the *next*
 * `EOS_Platform_Tick()` call afterward - nowhere near the actual bug.
 *
 * These tests invoke a registered stub's function pointer through a real
 * downcall - the exact mechanism native code uses to call back into Java -
 * so they exercise the real FFI boundary rather than just the Kotlin-level
 * lambda. They do not require the EOS native library: an upcall stub is a
 * JVM-generated native trampoline, independent of any externally loaded DLL.
 */
class CallbackStubsTest {

    @Test
    fun `an exception thrown inside a callback does not escape the upcall`() {
        var invoked = false
        val handle = CallbackStubs.register(
            EosCallback {
                invoked = true
                error("boom")
            }
        )
        try {
            // Before the fix, this call would either propagate the exception
            // in an unspecified way or crash the JVM outright.
            invokeStub(handle)
            assertTrue(invoked, "the callback body should still run before it throws")
        } finally {
            CallbackStubs.release(handle.id)
        }
    }

    @Test
    fun `a callback still runs normally after a previous callback threw`() {
        val throwing = CallbackStubs.register(EosCallback { error("boom") })
        var callCount = 0
        val normal = CallbackStubs.register(EosCallback { callCount++ })
        try {
            invokeStub(throwing)
            invokeStub(normal)
            invokeStub(normal)
            assertEquals(2, callCount, "the guard must not disable or corrupt other registered stubs")
        } finally {
            CallbackStubs.release(throwing.id)
            CallbackStubs.release(normal.id)
        }
    }

    @Test
    fun `many repeated exceptions across one stub do not crash or leak state`() {
        var callCount = 0
        val handle = CallbackStubs.register(
            EosCallback {
                callCount++
                throw IllegalStateException("simulated native failure-path bug")
            }
        )
        try {
            repeat(200) { invokeStub(handle) }
            assertEquals(200, callCount)
        } finally {
            CallbackStubs.release(handle.id)
        }
    }

    @Test
    fun `different exception types are all swallowed`() {
        val exceptions = listOf(
            { error("kotlin error()") },
            { throw NullPointerException("npe") },
            { throw IndexOutOfBoundsException("index") },
            { throw OutOfMemoryError("simulated error, not just exception") },
        )
        for (thrower in exceptions) {
            val handle = CallbackStubs.register(EosCallback { thrower() })
            try {
                invokeStub(handle)
            } finally {
                CallbackStubs.release(handle.id)
            }
        }
    }

    @Test
    fun `release cleans up normally for a stub that threw`() {
        val handle = CallbackStubs.register(EosCallback { error("boom") })
        invokeStub(handle)
        assertTrue(CallbackStubs.getStubSegment(handle.id) != null)
        CallbackStubs.release(handle.id)
        assertTrue(CallbackStubs.getStubSegment(handle.id) == null)
    }

    /** Invokes [handle]'s native function pointer via a real downcall, exactly as EOS's native code would. */
    private fun invokeStub(handle: StubHandle) {
        val downcall = Native.linker.downcallHandle(
            handle.segment,
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
        )
        downcall.invokeWithArguments(MemorySegment.NULL)
    }
}
