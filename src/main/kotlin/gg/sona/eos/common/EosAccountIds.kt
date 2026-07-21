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
package gg.sona.eos.common

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
public value class EpicAccountId(public val raw: Long) {
    public fun isValid(): Boolean {
        if (raw == 0L) return false
        val fn = Native.downcall(
            "EOS_EpicAccountId_IsValid",
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
                "EOS_EpicAccountId_ToString",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                )
            )
            fn.invokeExact(raw, MemorySegment.NULL, sizePtr)
            val size = sizePtr.get(ValueLayout.JAVA_INT, 0)
            val buf = arena.allocate(size.toLong())
            sizePtr.set(ValueLayout.JAVA_INT, 0, size)
            fn.invokeExact(raw, buf, sizePtr)
            buf.getString(0)
        }
    }

    public companion object {
        /** Construct an [EpicAccountId] from a previously-serialized string. */
        public fun fromString(value: String): EpicAccountId {
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
        public val Invalid: EpicAccountId = EpicAccountId(0L)
    }
}

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
            fn.invokeExact(raw, MemorySegment.NULL, sizePtr)
            val size = sizePtr.get(ValueLayout.JAVA_INT, 0)
            val buf = arena.allocate(size.toLong())
            sizePtr.set(ValueLayout.JAVA_INT, 0, size)
            fn.invokeExact(raw, buf, sizePtr)
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

/**
 * An opaque handle to a continuance token returned by some EOS Connect APIs.
 * Use [stringify] to serialize for later use.
 */
@JvmInline
public value class ContinuanceToken(public val raw: Long) {
    public fun isValid(): Boolean = raw != 0L

    public fun stringify(): String {
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
            fn.invokeExact(raw, MemorySegment.NULL, sizePtr)
            val size = sizePtr.get(ValueLayout.JAVA_INT, 0)
            val buf = arena.allocate(size.toLong())
            sizePtr.set(ValueLayout.JAVA_INT, 0, size)
            fn.invokeExact(raw, buf, sizePtr)
            buf.getString(0)
        }
    }
}
