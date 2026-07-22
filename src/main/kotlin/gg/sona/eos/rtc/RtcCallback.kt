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
package gg.sona.eos.rtc

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Helper functions to read fields from the C callback info struct that EOS
 * passes to a callback. The struct is laid out as documented for each
 * callback type in eos_rtc_types.h.
 */
internal object RtcCallback {
    /** Read a string pointer at [offset] within [data] as a Kotlin String, or null. */
    fun readString(data: MemorySegment, offset: Long): String? {
        val ptr = data.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return null
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }

    // EOS_RTC_JoinRoom/LeaveRoom/BlockParticipant/DisconnectedCallbackInfo: ResultCode@0
    fun readResult(data: MemorySegment): EosResult = EosResult.fromValue(data.getInt32(0))

    // EOS_RTC_JoinRoom/LeaveRoom/BlockParticipant/DisconnectedCallbackInfo: LocalUserId@16
    fun readLocalUserId(data: MemorySegment): ProductUserId = ProductUserId(data.getInt64(16))
}
