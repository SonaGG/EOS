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

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Holds stable C-callable function pointers (upcall stubs) for callbacks
 * passed to EOS.
 *
 * The C library retains the function pointer for the lifetime of a
 * subscription and invokes it from arbitrary threads. The FFM [Arena] backing
 * the upcall stub must therefore outlive the subscription, so we keep it in
 * this registry until [release] is called.
 */
internal object CallbackStubs {
    private val entries = ConcurrentHashMap<Long, Entry>()
    private val nextId = AtomicLong(1L shl 60)

    private class Entry(val stub: MemorySegment, val arena: Arena)

    /** Register a Kotlin functional interface as a C-callable function pointer. */
    fun register(functionalInterface: Any, descriptor: FunctionDescriptor = EosCallback.descriptor): StubHandle {
        val invoke = findPublicInvokeMethod(functionalInterface)
        val handle = MethodHandles.publicLookup().unreflect(invoke).bindTo(functionalInterface)
        val adapted = handle.asType(javaTypeFor(descriptor))
        val sized = reinterpretSegmentArguments(adapted)
        val guarded = guardAgainstUpcallExceptions(sized)
        val arena = Arena.ofShared()
        val stub = Native.linker.upcallStub(guarded, descriptor, arena)
        val id = nextId.getAndIncrement()
        entries[id] = Entry(stub, arena)
        return StubHandle(id, stub)
    }

    /**
     * A pointer parameter declared as a bare [ValueLayout.ADDRESS] arrives in an upcall as a
     * *zero-length* [MemorySegment]: the JVM knows the address but has no idea how much memory
     * is behind it, so every read fails with `IndexOutOfBoundsException ... byteSize: 0`.
     *
     * EOS hands every callback a pointer to a `..._CallbackInfo` struct it owns, so the size is
     * never known to us up front. Each such parameter is therefore re-interpreted as unbounded
     * before the callback body sees it, which is what makes `data.getInt32(0)` and friends work.
     */
    private fun reinterpretSegmentArguments(handle: MethodHandle): MethodHandle {
        var result = handle
        handle.type().parameterList().forEachIndexed { index, type ->
            if (type == MemorySegment::class.java) {
                result = MethodHandles.filterArguments(result, index, REINTERPRET_SEGMENT)
            }
        }
        return result
    }

    private val REINTERPRET_SEGMENT: MethodHandle = MethodHandles.lookup().findStatic(
        CallbackStubs::class.java,
        "reinterpretSegment",
        MethodType.methodType(MemorySegment::class.java, MemorySegment::class.java),
    )

    @Suppress("unused")
    @JvmStatic
    private fun reinterpretSegment(segment: MemorySegment): MemorySegment =
        if (segment.byteSize() == 0L && segment.address() != 0L) {
            segment.reinterpret(Long.MAX_VALUE)
        } else {
            segment
        }

    /**
     * A Java exception thrown while a callback is being invoked from native
     * code (an upcall) must never be allowed to propagate back into native
     * code - the FFM API contract leaves that case undefined, and in
     * practice it corrupts the upcall/downcall stub machinery and crashes
     * the JVM with an access violation on a later, unrelated native call
     * rather than failing cleanly at the throw site. Every callback is
     * therefore wrapped so exceptions are caught and logged here instead.
     */
    private fun guardAgainstUpcallExceptions(handle: MethodHandle): MethodHandle {
        val type = handle.type()
        check(type.returnType() == Void.TYPE) {
            "guardAgainstUpcallExceptions only supports void-returning callbacks, got $type"
        }
        val handler = MethodHandles.dropArguments(LOG_UPCALL_EXCEPTION, 1, type.parameterList())
        return MethodHandles.catchException(handle, Throwable::class.java, handler)
    }

    private val LOG_UPCALL_EXCEPTION: MethodHandle = MethodHandles.lookup().findStatic(
        CallbackStubs::class.java,
        "logUpcallException",
        MethodType.methodType(Void.TYPE, Throwable::class.java),
    )

    @Suppress("unused")
    @JvmStatic
    private fun logUpcallException(t: Throwable) {
        System.err.println(
            "[eos] Uncaught exception in a native callback was suppressed to avoid crossing " +
                "back into native code (undefined behavior) and crashing the JVM:"
        )
        t.printStackTrace()
    }

    /**
     * The runtime class of a Kotlin SAM lambda (e.g. `EosCallback { ... }`) is a
     * synthetic, non-public class, so [MethodHandles.publicLookup] cannot
     * unreflect a `Method` obtained from it even though the functional
     * interface it implements is public. Resolve `invoke` on that public
     * interface instead.
     */
    private fun findPublicInvokeMethod(functionalInterface: Any): java.lang.reflect.Method {
        val publicInterface = functionalInterface.javaClass.interfaces
            .firstOrNull { java.lang.reflect.Modifier.isPublic(it.modifiers) }
            ?: error("No public functional interface found for ${functionalInterface.javaClass}")
        return publicInterface.methods.first { it.name == "invoke" }
    }

    fun release(id: Long) {
        entries.remove(id)?.arena?.close()
    }

    fun getStubSegment(id: Long): MemorySegment? = entries[id]?.stub

    fun releaseAll() {
        val toClose = entries.values.toList()
        entries.clear()
        toClose.forEach { it.arena.close() }
    }

    private fun javaTypeFor(descriptor: FunctionDescriptor): MethodType {
        val argClasses: List<Class<*>> = descriptor.argumentLayouts().map { javaClassFor(it) }
        val retClass = javaClassFor(descriptor.returnLayout().orElse(null))
        return MethodType.methodType(retClass, argClasses.toTypedArray())
    }

    private fun javaClassFor(layout: MemoryLayout?): Class<*> = when (layout) {
        null -> java.lang.Void.TYPE
        is ValueLayout -> when (layout.carrier()) {
            java.lang.Boolean.TYPE -> java.lang.Boolean.TYPE
            java.lang.Byte.TYPE -> java.lang.Byte.TYPE
            java.lang.Short.TYPE -> java.lang.Short.TYPE
            java.lang.Character.TYPE -> java.lang.Integer.TYPE
            java.lang.Integer.TYPE -> java.lang.Integer.TYPE
            java.lang.Long.TYPE -> java.lang.Long.TYPE
            java.lang.Float.TYPE -> java.lang.Float.TYPE
            java.lang.Double.TYPE -> java.lang.Double.TYPE
            else -> MemorySegment::class.java
        }
        else -> MemorySegment::class.java
    }
}
