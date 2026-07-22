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
package gg.sona.eos.stats

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Stats interface. Ingest arbitrary integer stats for a player and query
 * them back later.
 */
class EosStats internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetStatsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Ingest one or more stats. Up to 3000 stats may be ingested in a single
     * call.
     */
    fun ingestStat(
        localUserId: ProductUserId?,
        targetUserId: ProductUserId,
        stats: List<IngestStat>,
    ): CompletableFuture<IngestStatResult> {
        require(stats.size <= MAX_INGEST_STATS) {
            "cannot ingest more than $MAX_INGEST_STATS stats at once"
        }
        val future = CompletableFuture<IngestStatResult>()
        // EOS_Stats_IngestStatCompleteCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val localUserId = ProductUserId(data.getInt64(16))
            val targetUserId = ProductUserId(data.getInt64(24))
            future.complete(IngestStatResult(result, localUserId, targetUserId))
        })
        val options = StatsIngestStatOptions(localUserId, targetUserId, stats)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Stats_IngestStat",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Query cached stats for a user. Returns a list of [PlayerStat] after
     * the operation completes successfully.
     */
    fun queryStats(
        localUserId: ProductUserId?,
        targetUserId: ProductUserId,
        statNames: List<String>? = null,
        startTime: Long = TIME_UNDEFINED,
        endTime: Long = TIME_UNDEFINED,
    ): CompletableFuture<QueryStatsResult> {
        val future = CompletableFuture<QueryStatsResult>()
        // EOS_Stats_OnQueryStatsCompleteCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val localUserId = ProductUserId(data.getInt64(16))
            val targetUserId = ProductUserId(data.getInt64(24))
            future.complete(QueryStatsResult(result, localUserId, targetUserId))
        })
        val options = StatsQueryStatsOptions(localUserId, targetUserId, statNames, startTime, endTime)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Stats_QueryStats",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun getStatsCount(targetUserId: ProductUserId): Int {
        val options = StatsGetStatsCountOptions(targetUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Stats_GetStatsCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    fun copyStatByIndex(targetUserId: ProductUserId, index: Int): PlayerStat? =
        withCallArena { arena ->
            val options = StatsCopyStatByIndexOptions(targetUserId, index)
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Stats_CopyStatByIndex",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val stat = readStat(seg)
            Native.downcall(
                "EOS_Stats_Stat_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            stat
        }

    fun copyStatByName(targetUserId: ProductUserId, name: String): PlayerStat? =
        withCallArena { arena ->
            val options = StatsCopyStatByNameOptions(targetUserId, name)
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Stats_CopyStatByName",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val stat = readStat(seg)
            Native.downcall(
                "EOS_Stats_Stat_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            stat
        }

    private fun readStat(seg: MemorySegment): PlayerStat {
        val ptr = seg.get(ValueLayout.ADDRESS, 8)
        val name = if (ptr.address() == 0L) "" else
            ptr.reinterpret(Long.MAX_VALUE).getString(0)
        return PlayerStat(
            name = name,
            startTime = seg.getInt64(16),
            endTime = seg.getInt64(24),
            value = seg.getInt32(32),
        )
    }

    companion object {
        const val MAX_INGEST_STATS: Int = 3000
        const val MAX_QUERY_STATS: Int = 1000
        const val TIME_UNDEFINED: Long = -1L
    }
}