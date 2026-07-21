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
        val arena = Arena.ofShared()
        val stub = Native.linker.upcallStub(adapted, descriptor, arena)
        val id = nextId.getAndIncrement()
        entries[id] = Entry(stub, arena)
        return StubHandle(id, stub)
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

/** A handle to a registered callback. */
internal class StubHandle internal constructor(
    @JvmField internal val id: Long,
    @JvmField internal val segment: MemorySegment,
)

/**
 * The shape of a typical EOS callback: one argument which is a pointer to a
 * callback-info struct, no return value. Implementations extract the relevant
 * fields from the supplied [MemorySegment] and call the user's lambda.
 */
fun interface EosCallback {
    fun invoke(data: MemorySegment)
    companion object {
        internal val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    }
}
