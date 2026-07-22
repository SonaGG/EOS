package gg.sona.eos.stats

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCStringArray
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

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