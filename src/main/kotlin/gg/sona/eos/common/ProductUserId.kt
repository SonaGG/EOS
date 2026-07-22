package gg.sona.eos.common

import gg.sona.eos.internal.Native
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * An opaque handle to a user's Product User ID (the EOS-managed per-product
 * user identifier that is independent of any specific external account).
 */
@JvmInline
public value class ProductUserId(public val raw: Long) {
    public fun isValid(): Boolean {
        if (raw == 0L) return false
        val fn = Native.downcall(
            "EOS_ProductUserId_IsValid",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
        )
        return (fn.invokeExact(raw) as Int) != 0
    }

    public override fun toString(): String = if (raw == 0L) "<invalid>" else toStringValue()

    public fun toStringValue(): String {
        if (raw == 0L) return ""
        return withCallArena { arena ->
            val sizePtr = arena.allocate(ValueLayout.JAVA_INT)
            val fn = Native.downcall(
                "EOS_ProductUserId_ToString",
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

    public companion object {
        /** Construct a [ProductUserId] from a previously-serialized string. */
        public fun fromString(value: String): ProductUserId {
            if (value.isEmpty()) return Invalid
            return withCallArena { arena ->
                val fn = Native.downcall(
                    "EOS_ProductUserId_FromString",
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
                )
                ProductUserId(fn.invokeExact(arena.allocCString(value)) as Long)
            }
        }

        /** The null/invalid sentinel. */
        public val Invalid: ProductUserId = ProductUserId(0L)
    }
}
