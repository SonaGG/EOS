package gg.sona.eos.stats

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class StatsIngestStatOptions(
    var localUserId: ProductUserId?,
    var targetUserId: ProductUserId,
    var stats: List<IngestStat>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        val dataSegs = stats.map {
            (it.statName to it.amount).let { (n, a) ->
                StatsIngestData(n, a).writeTo(arena)
            }
        }
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