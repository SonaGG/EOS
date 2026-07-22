package gg.sona.eos.leaderboards

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

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
