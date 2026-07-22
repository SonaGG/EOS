package gg.sona.eos.internal

import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

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
