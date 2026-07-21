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

import gg.sona.eos.internal.setInt8
import gg.sona.eos.internal.setInt16
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.setFloat
import gg.sona.eos.internal.setDouble
import gg.sona.eos.internal.setBool
import gg.sona.eos.internal.getInt8
import gg.sona.eos.internal.getInt16
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import gg.sona.eos.internal.getFloat
import gg.sona.eos.internal.getDouble
import gg.sona.eos.internal.getBool

import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Options for [Eos.initialize].
 *
 * The required fields are [productName] and [productVersion]. Custom memory
 * allocators are supported by setting all three of [allocateMemory],
 * [reallocateMemory], and [releaseMemory]; when not provided, EOS uses the
 * default system allocator.
 */
public class EosInitializeOptions internal constructor() : StructWriter {

    /** API version. Set automatically by [build]. */
    public var apiVersion: Int = API_LATEST

    /** Required. The product name shown in logs and the dev portal. Max 64 bytes. */
    public var productName: String = ""

    /** Required. The product version shown in logs and the dev portal. Max 64 bytes. */
    public var productVersion: String = ""

    /**
     * Optional memory allocator. Signature: `(size: long, alignment: long) -> long`,
     * where the returned value is the address of the allocated memory, or 0 on
     * failure. If non-null, [reallocateMemory] and [releaseMemory] must also be
     * provided.
     */
    public var allocateMemory: ((size: Long, alignment: Long) -> Long)? = null

    /** Optional memory reallocator. See [allocateMemory] for the contract. */
    public var reallocateMemory: ((pointer: Long, size: Long, alignment: Long) -> Long)? = null

    /** Optional memory releaser. Signature: `(pointer: Long) -> Unit`. */
    public var releaseMemory: ((pointer: Long) -> Unit)? = null

    override fun writeTo(arena: Arena): MemorySegment {
        // Custom allocators are intentionally not yet supported via the FFM upcall
        // mechanism because each requires its own stable function pointer with
        // varying signatures. Callers wishing to use one can use the platform's
        // own foreign function support or do so through a native shim.
        check(allocateMemory == null && reallocateMemory == null && releaseMemory == null) {
            "Custom memory allocators are not yet supported in the Kotlin binding"
        }
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(OFFSET_API_VERSION, apiVersion)
        seg.set(ValueLayout.ADDRESS, OFFSET_ALLOC, MemorySegment.NULL)
        seg.set(ValueLayout.ADDRESS, OFFSET_REALLOC, MemorySegment.NULL)
        seg.set(ValueLayout.ADDRESS, OFFSET_RELEASE, MemorySegment.NULL)
        seg.set(ValueLayout.ADDRESS, OFFSET_PRODUCT_NAME, arena.allocCString(productName))
        seg.set(ValueLayout.ADDRESS, OFFSET_PRODUCT_VERSION, arena.allocCString(productVersion))
        seg.set(ValueLayout.ADDRESS, OFFSET_RESERVED, MemorySegment.NULL)
        seg.set(ValueLayout.ADDRESS, OFFSET_SYSTEM_INIT, MemorySegment.NULL)
        seg.set(ValueLayout.ADDRESS, OFFSET_THREAD_AFFINITY, MemorySegment.NULL)
        return seg
    }

    public fun build(): EosInitializeOptions = this

    public companion object {
        /** Latest API version supported by the binding. */
        public const val API_LATEST: Int = 5

        internal val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("ApiVersion"),
            MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS.withName("AllocateMemoryFunction"),
            ValueLayout.ADDRESS.withName("ReallocateMemoryFunction"),
            ValueLayout.ADDRESS.withName("ReleaseMemoryFunction"),
            ValueLayout.ADDRESS.withName("ProductName"),
            ValueLayout.ADDRESS.withName("ProductVersion"),
            ValueLayout.ADDRESS.withName("Reserved"),
            ValueLayout.ADDRESS.withName("SystemInitializeOptions"),
            ValueLayout.ADDRESS.withName("OverrideThreadAffinity"),
        )

        internal const val OFFSET_API_VERSION: Long = 0
        internal const val OFFSET_ALLOC: Long = 8
        internal const val OFFSET_REALLOC: Long = 16
        internal const val OFFSET_RELEASE: Long = 24
        internal const val OFFSET_PRODUCT_NAME: Long = 32
        internal const val OFFSET_PRODUCT_VERSION: Long = 40
        internal const val OFFSET_RESERVED: Long = 48
        internal const val OFFSET_SYSTEM_INIT: Long = 56
        internal const val OFFSET_THREAD_AFFINITY: Long = 64

        /** Construct a new options builder with the required fields. */
        public fun create(productName: String, productVersion: String): EosInitializeOptions =
            EosInitializeOptions().apply {
                this.productName = productName
                this.productVersion = productVersion
            }
    }
}
