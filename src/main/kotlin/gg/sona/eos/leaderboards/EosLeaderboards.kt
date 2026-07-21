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
package gg.sona.eos.leaderboards

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.allocHandleArray
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import gg.sona.eos.internal.getUInt32
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
 * Leaderboards interface.
 */
public class EosLeaderboards internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetLeaderboardsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /** Query leaderboard definitions from the backend. */
    public fun queryLeaderboardDefinitions(
        localUserId: ProductUserId? = null,
        startTime: Long = TIME_UNDEFINED,
        endTime: Long = TIME_UNDEFINED,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = LeaderboardsQueryLeaderboardDefinitionsOptions(
            startTime, endTime, localUserId ?: ProductUserId.Invalid
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Leaderboards_QueryLeaderboardDefinitions",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getLeaderboardDefinitionCount(): Int {
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

    public fun copyLeaderboardDefinitionByIndex(index: Int): LeaderboardDefinition? =
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

    public fun copyLeaderboardDefinitionByLeaderboardId(
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

    public fun queryLeaderboardRanks(
        leaderboardId: String,
        localUserId: ProductUserId? = null,
    ): CompletableFuture<QueryLeaderboardRanksResult> {
        val future = CompletableFuture<QueryLeaderboardRanksResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val lbId = data.getInt64(24).let { addr ->
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
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getLeaderboardRecordCount(): Int {
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

    public fun copyLeaderboardRecordByIndex(index: Int): LeaderboardRecord? =
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

    public fun copyLeaderboardRecordByUserId(userId: ProductUserId): LeaderboardRecord? =
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

    public fun queryLeaderboardUserScores(
        userIds: List<ProductUserId>,
        statInfo: List<UserScoresQueryStatInfo>,
        localUserId: ProductUserId? = null,
        startTime: Long = TIME_UNDEFINED,
        endTime: Long = TIME_UNDEFINED,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = LeaderboardsQueryLeaderboardUserScoresOptions(
            userIds, statInfo, startTime, endTime, localUserId ?: ProductUserId.Invalid
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Leaderboards_QueryLeaderboardUserScores",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getLeaderboardUserScoreCount(statName: String): Int {
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

    public fun copyLeaderboardUserScoreByIndex(
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

    public fun copyLeaderboardUserScoreByUserId(
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

    public companion object {
        public const val TIME_UNDEFINED: Long = -1L
    }
}

public enum class LeaderboardAggregation(val value: Int) {
    Min(0),
    Max(1),
    Sum(2),
    Latest(3);

    public companion object {
        internal fun fromValue(v: Int): LeaderboardAggregation =
            entries.firstOrNull { it.value == v } ?: Min
    }
}

public class LeaderboardDefinition(
    public val leaderboardId: String,
    public val statName: String,
    public val aggregation: LeaderboardAggregation,
    public val startTime: Long,
    public val endTime: Long,
)

public class LeaderboardRecord(
    public val userId: ProductUserId,
    public val rank: UInt,
    public val score: Int,
    public val userDisplayName: String,
)

public class LeaderboardUserScore(
    public val userId: ProductUserId,
    public val score: Int,
)

public class UserScoresQueryStatInfo(public val statName: String, public val aggregation: LeaderboardAggregation)

public class QueryLeaderboardRanksResult(
    public val result: EosResult,
    public val leaderboardId: String,
)

// region Struct writers

internal class LeaderboardsQueryLeaderboardDefinitionsOptions(
    var startTime: Long,
    var endTime: Long,
    var localUserId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, startTime)
        seg.setInt64(16, endTime)
        seg.setInt64(24, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class LeaderboardsGetLeaderboardDefinitionCountOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4)
        )
    }
}

internal class LeaderboardsCopyLeaderboardDefinitionByIndexOptions(var index: Int) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt32(8, index)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_INT
        )
    }
}

internal class LeaderboardsCopyLeaderboardDefinitionByLeaderboardIdOptions(
    var leaderboardId: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(leaderboardId).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class LeaderboardsQueryLeaderboardRanksOptions(
    var leaderboardId: String,
    var localUserId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(leaderboardId).address())
        seg.setInt64(16, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
        )
    }
}

internal class LeaderboardsGetLeaderboardRecordCountOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4)
        )
    }
}

internal class LeaderboardsCopyLeaderboardRecordByIndexOptions(var index: Int) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt32(8, index)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_INT
        )
    }
}

internal class LeaderboardsCopyLeaderboardRecordByUserIdOptions(
    var userId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, userId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}

internal class LeaderboardsQueryLeaderboardUserScoresOptions(
    var userIds: List<ProductUserId>,
    var statInfo: List<UserScoresQueryStatInfo>,
    var startTime: Long,
    var endTime: Long,
    var localUserId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        val userArr = arena.allocHandleArray(userIds.map { it.raw })
        seg.setInt64(8, userArr.address())
        seg.setInt32(16, userIds.size)
        val statArr = arena.allocate(LAYOUT_STAT, statInfo.size.toLong())
        statInfo.forEachIndexed { i, s ->
            val item = statArr.asSlice(i.toLong() * LAYOUT_STAT.byteSize(), LAYOUT_STAT.byteSize())
            item.setInt32(0, 1) // API
            item.setInt64(8, arena.allocCString(s.statName).address())
            item.setInt32(16, s.aggregation.value)
        }
        seg.setInt64(24, statArr.address())
        seg.setInt32(32, statInfo.size)
        seg.setInt64(40, startTime)
        seg.setInt64(48, endTime)
        seg.setInt64(56, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
        val LAYOUT_STAT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class LeaderboardsGetLeaderboardUserScoreCountOptions(var statName: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(statName).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class LeaderboardsCopyLeaderboardUserScoreByIndexOptions(
    var index: Int,
    var statName: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt32(8, index)
        seg.setInt64(16, arena.allocCString(statName).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}

internal class LeaderboardsCopyLeaderboardUserScoreByUserIdOptions(
    var userId: ProductUserId,
    var statName: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, userId.raw)
        seg.setInt64(16, arena.allocCString(statName).address())
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
