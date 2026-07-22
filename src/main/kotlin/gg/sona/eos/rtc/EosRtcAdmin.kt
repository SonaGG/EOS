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

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Server-side RTC administration: query join tokens, kick, hard-mute.
 *
 * This interface is intended to be used from a trusted server process. The
 * server's platform options must have the appropriate client credentials.
 */
class EosRtcAdmin internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetRTCAdminInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Query for a batch of user tokens that can be used by the supplied user
     * ids to join a room. The returned tokens should be distributed to the
     * corresponding clients.
     */
    fun queryJoinRoomToken(
        localUserId: ProductUserId,
        roomName: String,
        targetUserIds: List<ProductUserId>,
        targetUserIpAddresses: List<String?>? = null,
    ): CompletableFuture<QueryJoinRoomTokenResult> {
        val future = CompletableFuture<QueryJoinRoomTokenResult>()
        // EOS_RTCAdmin_QueryJoinRoomTokenCompleteCallbackInfo: ResultCode@0, ClientData@8, RoomName@16,
        // ClientBaseUrl@24, QueryId@32, TokenCount@36
        val invoker = EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val room = readCString(data, 16) ?: ""
            val clientBaseUrl = readCString(data, 24) ?: ""
            val queryId = data.getInt32(32).toLong() and 0xffffffffL
            val tokenCount = data.getInt32(36).toLong() and 0xffffffffL
            future.complete(QueryJoinRoomTokenResult(result, room, clientBaseUrl, queryId, tokenCount.toInt()))
        }
        val stub = CallbackStubs.register(invoker)
        val options = RtcAdminQueryJoinRoomTokenOptions(
            localUserId, roomName,
            targetUserIds.map { it.raw },
            targetUserIpAddresses,
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAdmin_QueryJoinRoomToken",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Fetches a user token by index. Call only inside the
     * [queryJoinRoomToken] completion callback.
     */
    fun copyUserTokenByIndex(queryId: Long, userTokenIndex: Int): RtcAdminUserToken? {
        return withCallArena { arena ->
            val options = RtcAdminCopyUserTokenByIndexOptions(userTokenIndex, queryId)
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_RTCAdmin_CopyUserTokenByIndex",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val token = RtcAdminUserToken(
                ProductUserId(seg.getInt64(8)),
                seg.reinterpret(Long.MAX_VALUE).getString(16),
            )
            // Release the C-side allocation
            val releaseFn = Native.downcall(
                "EOS_RTCAdmin_UserToken_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            )
            releaseFn.invokeExact(seg)
            token
        }
    }

    fun copyUserTokenByUserId(queryId: Long, targetUserId: ProductUserId): RtcAdminUserToken? {
        return withCallArena { arena ->
            val options = RtcAdminCopyUserTokenByUserIdOptions(targetUserId.raw, queryId)
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_RTCAdmin_CopyUserTokenByUserId",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val token = RtcAdminUserToken(
                ProductUserId(seg.getInt64(8)),
                seg.reinterpret(Long.MAX_VALUE).getString(16),
            )
            val releaseFn = Native.downcall(
                "EOS_RTCAdmin_UserToken_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            )
            releaseFn.invokeExact(seg)
            token
        }
    }

    fun kick(roomName: String, targetUserId: ProductUserId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAdmin_KickCompleteCallbackInfo: ResultCode@0
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAdminKickOptions(roomName, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAdmin_Kick",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun setParticipantHardMute(
        roomName: String,
        targetUserId: ProductUserId,
        mute: Boolean,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAdmin_SetParticipantHardMuteCompleteCallbackInfo: ResultCode@0
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAdminSetParticipantHardMuteOptions(roomName, targetUserId, mute)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAdmin_SetParticipantHardMute",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }
}
