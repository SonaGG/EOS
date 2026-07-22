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
package gg.sona.eos.leaderboards

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Leaderboards interface.
 */
class EosLeaderboards internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetLeaderboardsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /** Query leaderboard definitions from the backend. */
    fun queryLeaderboardDefinitions(
        localUserId: ProductUserId? = null,
        startTime: Long = TIME_UNDEFINED,
        endTime: Long = TIME_UNDEFINED,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Leaderboards_OnQueryLeaderboardDefinitionsCompleteCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LeaderboardsQueryLeaderboardDefinitionsOptions(
            startTime, endTime, localUserId ?: ProductUserId.Invalid
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Leaderboards_QueryLeaderboardDefinitions",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun getLeaderboardDefinitionCount(): Int {
        val options = LeaderboardsGetLeaderboardDefinitionCountOptions()
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Leaderboards_GetLeaderboardDefinitionCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    fun copyLeaderboardDefinitionByIndex(index: Int): LeaderboardDefinition? =
        withCallArena { arena ->
            val options = LeaderboardsCopyLeaderboardDefinitionByIndexOptions(index)
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Leaderboards_CopyLeaderboardDefinitionByIndex",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val def = readDefinition(seg)
            Native.downcall(
                "EOS_Leaderboards_Definition_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            def
        }

    fun copyLeaderboardDefinitionByLeaderboardId(
        leaderboardId: String,
    ): LeaderboardDefinition? = withCallArena { arena ->
        val options = LeaderboardsCopyLeaderboardDefinitionByLeaderboardIdOptions(leaderboardId)
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_Leaderboards_CopyLeaderboardDefinitionByLeaderboardId",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) return@withCallArena null
        val def = readDefinition(seg)
        Native.downcall(
            "EOS_Leaderboards_Definition_Release",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        ).invokeExact(seg)
        def
    }

    fun queryLeaderboardRanks(
        leaderboardId: String,
        localUserId: ProductUserId? = null,
    ): CompletableFuture<QueryLeaderboardRanksResult> {
        val future = CompletableFuture<QueryLeaderboardRanksResult>()
        // EOS_Leaderboards_OnQueryLeaderboardRanksCompleteCallbackInfo: ResultCode@0, ClientData@8, LeaderboardId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val lbId = data.getInt64(16).let { addr ->
                if (addr == 0L) "" else
                    MemorySegment.ofAddress(addr).reinterpret(Long.MAX_VALUE).getString(0)
            }
            future.complete(QueryLeaderboardRanksResult(result, lbId))
        })
        val options = LeaderboardsQueryLeaderboardRanksOptions(leaderboardId, localUserId ?: ProductUserId.Invalid)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Leaderboards_QueryLeaderboardRanks",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun getLeaderboardRecordCount(): Int {
        val options = LeaderboardsGetLeaderboardRecordCountOptions()
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Leaderboards_GetLeaderboardRecordCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    fun copyLeaderboardRecordByIndex(index: Int): LeaderboardRecord? =
        withCallArena { arena ->
            val options = LeaderboardsCopyLeaderboardRecordByIndexOptions(index)
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Leaderboards_CopyLeaderboardRecordByIndex",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val rec = readRecord(seg)
            Native.downcall(
                "EOS_Leaderboards_LeaderboardRecord_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            rec
        }

    fun copyLeaderboardRecordByUserId(userId: ProductUserId): LeaderboardRecord? =
        withCallArena { arena ->
            val options = LeaderboardsCopyLeaderboardRecordByUserIdOptions(userId)
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Leaderboards_CopyLeaderboardRecordByUserId",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val rec = readRecord(seg)
            Native.downcall(
                "EOS_Leaderboards_LeaderboardRecord_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            rec
        }

    fun queryLeaderboardUserScores(
        userIds: List<ProductUserId>,
        statInfo: List<UserScoresQueryStatInfo>,
        localUserId: ProductUserId? = null,
        startTime: Long = TIME_UNDEFINED,
        endTime: Long = TIME_UNDEFINED,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Leaderboards_OnQueryLeaderboardUserScoresCompleteCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LeaderboardsQueryLeaderboardUserScoresOptions(
            userIds, statInfo, startTime, endTime, localUserId ?: ProductUserId.Invalid
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Leaderboards_QueryLeaderboardUserScores",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun getLeaderboardUserScoreCount(statName: String): Int {
        val options = LeaderboardsGetLeaderboardUserScoreCountOptions(statName)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Leaderboards_GetLeaderboardUserScoreCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    fun copyLeaderboardUserScoreByIndex(
        statName: String,
        index: Int,
    ): LeaderboardUserScore? = withCallArena { arena ->
        val options = LeaderboardsCopyLeaderboardUserScoreByIndexOptions(index, statName)
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_Leaderboards_CopyLeaderboardUserScoreByIndex",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) return@withCallArena null
        val rec = readUserScore(seg)
        Native.downcall(
            "EOS_Leaderboards_LeaderboardUserScore_Release",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        ).invokeExact(seg)
        rec
    }

    fun copyLeaderboardUserScoreByUserId(
        statName: String,
        userId: ProductUserId,
    ): LeaderboardUserScore? = withCallArena { arena ->
        val options = LeaderboardsCopyLeaderboardUserScoreByUserIdOptions(userId, statName)
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_Leaderboards_CopyLeaderboardUserScoreByUserId",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) return@withCallArena null
        val rec = readUserScore(seg)
        Native.downcall(
            "EOS_Leaderboards_LeaderboardUserScore_Release",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        ).invokeExact(seg)
        rec
    }

    private fun readString(seg: MemorySegment, offset: Long): String {
        val ptr = seg.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return ""
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }

    private fun readDefinition(seg: MemorySegment): LeaderboardDefinition = LeaderboardDefinition(
        leaderboardId = readString(seg, 8),
        statName = readString(seg, 16),
        aggregation = LeaderboardAggregation.fromValue(seg.getInt32(24)),
        startTime = seg.getInt64(32),
        endTime = seg.getInt64(40),
    )

    private fun readRecord(seg: MemorySegment): LeaderboardRecord = LeaderboardRecord(
        userId = ProductUserId(seg.getInt64(8)),
        rank = (seg.getInt32(16).toLong() and 0xffffffffL).toUInt(),
        score = seg.getInt32(24),
        userDisplayName = readString(seg, 32),
    )

    private fun readUserScore(seg: MemorySegment): LeaderboardUserScore = LeaderboardUserScore(
        userId = ProductUserId(seg.getInt64(8)),
        score = seg.getInt32(16),
    )

    companion object {
        const val TIME_UNDEFINED: Long = -1L
    }
}