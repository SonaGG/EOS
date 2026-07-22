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
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Implemented by all Kotlin types that can be marshalled into a native EOS
 * struct. Implementations write the struct's fields into the supplied segment
 * using the provided arena for any auxiliary allocations (strings, arrays).
 *
 * The EOS structs all use `#pragma pack(8)`, so implementations must use
 * [ValueLayout] of the appropriate byte width without padding.
 */
@PublishedApi
internal interface StructWriter {
    fun writeTo(arena: Arena): MemorySegment
}

// Extension functions for writing/reading primitive values at a given offset.
// These are top-level functions in the internal package; the relevant modules
// import them individually.

// region Writers

fun MemorySegment.setInt8(offset: Long, value: Byte) = set(ValueLayout.JAVA_BYTE, offset, value)
fun MemorySegment.setInt16(offset: Long, value: Short) = set(ValueLayout.JAVA_SHORT, offset, value)
fun MemorySegment.setInt32(offset: Long, value: Int) = set(ValueLayout.JAVA_INT, offset, value)
fun MemorySegment.setInt64(offset: Long, value: Long) = set(ValueLayout.JAVA_LONG, offset, value)
fun MemorySegment.setUInt8(offset: Long, value: Int) = set(ValueLayout.JAVA_BYTE, offset, value.toByte())
fun MemorySegment.setUInt16(offset: Long, value: Int) = set(ValueLayout.JAVA_SHORT, offset, value.toShort())
fun MemorySegment.setUInt32(offset: Long, value: Long) = set(ValueLayout.JAVA_INT, offset, value.toInt())
fun MemorySegment.setFloat(offset: Long, value: Float) = set(ValueLayout.JAVA_FLOAT, offset, value)
fun MemorySegment.setDouble(offset: Long, value: Double) = set(ValueLayout.JAVA_DOUBLE, offset, value)
fun MemorySegment.setBool(offset: Long, value: Boolean) = set(ValueLayout.JAVA_INT, offset, if (value) 1 else 0)

fun MemorySegment.getInt8(offset: Long): Byte = get(ValueLayout.JAVA_BYTE, offset)
fun MemorySegment.getInt16(offset: Long): Short = get(ValueLayout.JAVA_SHORT, offset)
fun MemorySegment.getInt32(offset: Long): Int = get(ValueLayout.JAVA_INT, offset)
fun MemorySegment.getInt64(offset: Long): Long = get(ValueLayout.JAVA_LONG, offset)
fun MemorySegment.getUInt8(offset: Long): Int = (get(ValueLayout.JAVA_BYTE, offset).toInt() and 0xff)
fun MemorySegment.getUInt16(offset: Long): Int = (get(ValueLayout.JAVA_SHORT, offset).toInt() and 0xffff)
fun MemorySegment.getUInt32(offset: Long): Long = (get(ValueLayout.JAVA_INT, offset).toLong() and 0xffffffffL)
fun MemorySegment.getFloat(offset: Long): Float = get(ValueLayout.JAVA_FLOAT, offset)
fun MemorySegment.getDouble(offset: Long): Double = get(ValueLayout.JAVA_DOUBLE, offset)
fun MemorySegment.getBool(offset: Long): Boolean = get(ValueLayout.JAVA_INT, offset) != 0

// endregion
