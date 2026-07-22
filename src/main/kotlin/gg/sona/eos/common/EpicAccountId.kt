package gg.sona.eos.common

import gg.sona.eos.common.EpicAccountId.Companion.fromString
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * An opaque handle to a user's Epic Account ID.
 *
 * Use [isValid] to check whether the handle points to an actual account. The
 * [fromString] factory is the safe way to convert from a string; EOS does not
 * validate the string format itself.
 *
 * The stringified representation is at most 32 bytes plus a null terminator.
 */
@JvmInline
value class EpicAccountId(val raw: Long) {
    fun isValid(): Boolean {
        if (raw == 0L) return false
        val fn = Native.downcall(
            "EOS_EpicAccountId_IsValid",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
        )
        return (fn.invokeExact(raw) as Int) != 0
    }

    override fun toString(): String = if (raw == 0L) "<invalid>" else toStringValue()

    fun toStringValue(): String {
        if (raw == 0L) return ""
        return withCallArena { arena ->
            val sizePtr = arena.allocate(ValueLayout.JAVA_INT)
            val fn = Native.downcall(
                "EOS_EpicAccountId_ToString",
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

    companion object {
        /** Construct an [EpicAccountId] from a previously-serialized string. */
        fun fromString(value: String): EpicAccountId {
            if (value.isEmpty()) return Invalid
            return withCallArena { arena ->
                val fn = Native.downcall(
                    "EOS_EpicAccountId_FromString",
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
                )
                EpicAccountId(fn.invokeExact(arena.allocCString(value)) as Long)
            }
        }

        /** The null/invalid sentinel. */
        val Invalid: EpicAccountId = EpicAccountId(0L)
    }
}
