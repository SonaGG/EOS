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
 * Helper for the "out struct pointer" pattern used throughout the EOS C API.
 *
 * The C function receives a pointer to a pointer (an `out` parameter) and
 * fills it with a newly allocated struct. The caller then calls a
 * type-specific release function. The pattern is:
 *
 *   EOS_EResul EOS_XX_Copy(opts, &out);
 *   // use out
 *   EOS_XX_Release(out);
 *
 * This helper runs the two-step call (one for the size, one for the data)
 * and then forwards the resulting pointer to the supplied [release] function
 * so the C side can free its allocation.
 */
internal object OutParam {

    /**
     * Allocate a [MemorySegment] of the supplied [valueLayout], call the
     * EOS function once with a non-null pointer, and return the populated
     * segment. The caller is responsible for releasing the C-side
     * allocation through [release].
     */
    inline fun <T> capture(
        arena: Arena,
        valueLayout: ValueLayout,
        call: (MemorySegment) -> Int,
        release: (MemorySegment) -> Unit,
        parse: (MemorySegment) -> T,
    ): T {
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val result = call(outPtr)
        require(result == 0) { "out param call failed with code $result" }
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) {
            throw IllegalStateException("EOS returned null out param despite success")
        }
        try {
            return parse(seg)
        } finally {
            release(seg)
        }
    }
}
