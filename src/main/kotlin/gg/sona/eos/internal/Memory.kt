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
 * Common helpers for reading and writing EOS memory buffers.
 */
internal object Memory {

    /**
     * Read a null-terminated UTF-8 string at [offset] in the data segment, or
     * null if the pointer at that offset is null.
     */
    fun readCString(data: MemorySegment, offset: Long): String? {
        val ptr = data.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return null
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }

    /**
     * Two-call string-out pattern used by many EOS APIs: first call with
     * [MemorySegment.NULL] to obtain the required buffer size, second call
     * with the actual buffer.
     */
    inline fun <T> withCStringBuffer(
        arena: Arena,
        call: (MemorySegment, MemorySegment) -> Int,
        parse: (String) -> T,
    ): T {
        val sizePtr = arena.allocate(ValueLayout.JAVA_INT)
        call.invoke(MemorySegment.NULL, sizePtr)
        val size = sizePtr.get(ValueLayout.JAVA_INT, 0)
        val buf = arena.allocate(size.toLong())
        sizePtr.set(ValueLayout.JAVA_INT, 0, size)
        val result = call.invoke(buf, sizePtr)
        require(result == 0) { "string out failed with code $result" }
        return parse(buf.getString(0))
    }
}
