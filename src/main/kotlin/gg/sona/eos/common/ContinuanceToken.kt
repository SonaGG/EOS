package gg.sona.eos.common

import gg.sona.eos.internal.Native
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * An opaque handle to a continuance token returned by some EOS Connect APIs.
 * Use [stringify] to serialize for later use.
 */
@JvmInline
value class ContinuanceToken(val raw: Long) {
    fun isValid(): Boolean = raw != 0L

    fun stringify(): String {
        if (raw == 0L) return ""
        return withCallArena { arena ->
            val sizePtr = arena.allocate(ValueLayout.JAVA_INT)
            val fn = Native.downcall(
                "EOS_ContinuanceToken_ToString",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                )
            )
            fn.invokeExact(raw, MemorySegment.NULL, sizePtr) as Int
            val size = sizePtr.get(ValueLayout.JAVA_INT, 0)
            val buf = arena.allocate(size.toLong())
            sizePtr.set(ValueLayout.JAVA_INT, 0, size)
            fn.invokeExact(raw, buf, sizePtr) as Int
            buf.getString(0)
        }
    }
}
