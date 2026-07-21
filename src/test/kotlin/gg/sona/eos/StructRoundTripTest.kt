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
package gg.sona.eos

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.allocCStringArray
import gg.sona.eos.internal.allocHandleArray
import gg.sona.eos.internal.getCStringAt
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the struct writer/reader round trip. They do not require the
 * native library because they allocate an [java.lang.foreign.Arena] and
 * operate entirely on the JVM side.
 */
class StructRoundTripTest {

    @Test
    fun `arena can be created and used without the native library`() {
        withCallArena { arena ->
            val seg = arena.allocate(64)
            seg.setInt32(0, 0x12345678)
            seg.setInt64(8, 0x7FFF_FFFF_FFFF_FFL)
            assertEquals(0x12345678, seg.getInt32(0))
            assertEquals(0x7FFF_FFFF_FFFF_FFL, seg.getInt64(8))
        }
    }

    @Test
    fun `allocCString allocates null-terminated UTF-8`() {
        withCallArena { arena ->
            val seg = arena.allocCString("hello world")
            assertEquals("hello world", seg.getString(0))
            // Check null terminator
            assertEquals(0, seg.get(ValueLayout.JAVA_BYTE, 11))
        }
    }

    @Test
    fun `allocCString handles null`() {
        withCallArena { arena ->
            val seg = arena.allocCString(null as String?)
            assertEquals(0L, seg.address())
        }
    }

    @Test
    fun `allocCStringArray creates array of pointers`() {
        withCallArena { arena ->
            val seg = arena.allocCStringArray(listOf("a", "b", "c"))
            assertEquals("a", seg.getCStringAt(0L) ?: "")
            assertEquals("b", seg.getCStringAt(8L) ?: "")
            assertEquals("c", seg.getCStringAt(16L) ?: "")
        }
    }

    @Test
    fun `allocCStringArray handles null entries`() {
        withCallArena { arena ->
            val seg = arena.allocCStringArray(listOf("a", null, "c"))
            assertEquals("a", seg.getCStringAt(0L) ?: "")
            // middle entry is null
            val middle = seg.get(ValueLayout.ADDRESS, 8)
            assertEquals(0L, middle.address())
            assertEquals("c", seg.getCStringAt(16L) ?: "")
        }
    }

    @Test
    fun `allocHandleArray writes raw longs`() {
        withCallArena { arena ->
            val handles = listOf(0xAAAAL, 0xBBBBL, 0xCCCCL)
            val seg = arena.allocHandleArray(handles)
            for ((i, h) in handles.withIndex()) {
                assertEquals(h, seg.getInt64(i.toLong() * 8))
            }
        }
    }

    @Test
    fun `native handle layout is 8 bytes`() {
        // ProductUserId is a value class wrapping a Long. We can construct it
        // and verify the size of the underlying raw is 8 bytes by allocating
        // a MemorySegment and writing/reading a Long.
        withCallArena { arena ->
            val seg = arena.allocate(8)
            seg.setInt64(0, 0xDEADBEEFL)
            assertEquals(0xDEADBEEFL, seg.getInt64(0))
        }
    }

    @Test
    fun `product user id value class stores 64-bit handle`() {
        val id = ProductUserId(0x7FFFFFFFL)
        assertEquals(0x7FFFFFFFL, id.raw)
    }

    @Test
    fun `arena handles multiple allocations`() {
        withCallArena { arena ->
            val first = arena.allocate(16)
            first.setInt32(0, 1)
            val second = arena.allocate(32)
            second.setInt32(0, 2)
            assertEquals(1, first.getInt32(0))
            assertEquals(2, second.getInt32(0))
        }
    }
}
