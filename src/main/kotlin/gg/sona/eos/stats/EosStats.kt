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
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.allocCStringArray
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Stats interface. Ingest arbitrary integer stats for a player and query
 * them back later.
 */
public class EosStats internal constructor(private val platform: EosPlatform) {

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
    public fun ingestStat(
        localUserId: ProductUserId?,
        targetUserId: ProductUserId,
        stats: List<IngestStat>,
    ): CompletableFuture<IngestStatResult> {
        require(stats.size <= MAX_INGEST_STATS) {
            "cannot ingest more than $MAX_INGEST_STATS stats at once"
        }
        val future = CompletableFuture<IngestStatResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = ProductUserId(data.getInt64(16))
            val targetUserId = ProductUserId(data.getInt64(24))
            future.complete(IngestStatResult(result, localUserId, targetUserId))
        })
        val options = StatsIngestStatOptions(localUserId, targetUserId, stats)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Stats_IngestStat",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Query cached stats for a user. Returns a list of [PlayerStat] after
     * the operation completes successfully.
     */
    public fun queryStats(
        localUserId: ProductUserId?,
        targetUserId: ProductUserId,
        statNames: List<String>? = null,
        startTime: Long = TIME_UNDEFINED,
        endTime: Long = TIME_UNDEFINED,
    ): CompletableFuture<QueryStatsResult> {
        val future = CompletableFuture<QueryStatsResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = ProductUserId(data.getInt64(16))
            val targetUserId = ProductUserId(data.getInt64(24))
            future.complete(QueryStatsResult(result, localUserId, targetUserId))
        })
        val options = StatsQueryStatsOptions(localUserId, targetUserId, statNames, startTime, endTime)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Stats_QueryStats",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getStatsCount(targetUserId: ProductUserId): Int {
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

    public fun copyStatByIndex(targetUserId: ProductUserId, index: Int): PlayerStat? =
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

    public fun copyStatByName(targetUserId: ProductUserId, name: String): PlayerStat? =
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

    public companion object {
        public const val MAX_INGEST_STATS: Int = 3000
        public const val MAX_QUERY_STATS: Int = 1000
        public const val TIME_UNDEFINED: Long = -1L
    }
}

/** A single stat to ingest. */
public class IngestStat(public val statName: String, public val amount: Int)

/** A cached stat for a user. */
public class PlayerStat(
    public val name: String,
    public val startTime: Long,
    public val endTime: Long,
    public val value: Int,
)

public class IngestStatResult(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val targetUserId: ProductUserId,
)

public class QueryStatsResult(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val targetUserId: ProductUserId,
)

// region Struct writers

internal class StatsIngestData(public val statName: String, public val ingestAmount: Int) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(statName).address())
        seg.setInt32(16, ingestAmount)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class StatsIngestStatOptions(
    var localUserId: ProductUserId?,
    var targetUserId: ProductUserId,
    var stats: List<IngestStat>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        val dataSegs = stats.map { (it.statName to it.amount).let { (n, a) ->
            StatsIngestData(n, a).writeTo(arena)
        } }
        val arr = arena.allocate(StatsIngestData.LAYOUT, dataSegs.size.toLong())
        dataSegs.forEachIndexed { i, s ->
            val dst = arr.asSlice(i.toLong() * StatsIngestData.LAYOUT.byteSize(), StatsIngestData.LAYOUT.byteSize())
            MemorySegment.copy(s, 0, dst, 0, StatsIngestData.LAYOUT.byteSize())
        }
        seg.setInt64(16, arr.address())
        seg.setInt32(24, dataSegs.size)
        seg.setInt64(32, targetUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 3
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class StatsQueryStatsOptions(
    var localUserId: ProductUserId?,
    var targetUserId: ProductUserId,
    var statNames: List<String>?,
    var startTime: Long,
    var endTime: Long,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        seg.setInt64(16, startTime)
        seg.setInt64(24, endTime)
        val sn = statNames
        if (sn != null) {
            val arr = arena.allocCStringArray(sn)
            seg.setInt64(32, arr.address())
            seg.setInt32(40, sn.size)
        } else {
            seg.setInt64(32, 0L)
            seg.setInt32(40, 0)
        }
        seg.setInt64(48, targetUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 3
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class StatsGetStatsCountOptions(var targetUserId: ProductUserId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, targetUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}

internal class StatsCopyStatByIndexOptions(
    var targetUserId: ProductUserId,
    var index: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, targetUserId.raw)
        seg.setInt32(16, index)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class StatsCopyStatByNameOptions(
    var targetUserId: ProductUserId,
    var name: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, targetUserId.raw)
        seg.setInt64(16, arena.allocCString(name).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

// endregion
