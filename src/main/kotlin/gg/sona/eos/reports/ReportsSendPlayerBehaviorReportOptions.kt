package gg.sona.eos.reports

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class ReportsSendPlayerBehaviorReportOptions(
    var reporterUserId: ProductUserId,
    var reportedUserId: ProductUserId,
    var reasonCategory: String,
    var comments: String,
    var contextLength: Int,
    var context: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, reporterUserId.raw)
        seg.setInt64(16, reportedUserId.raw)
        seg.setInt64(24, arena.allocCString(reasonCategory).address())
        seg.setInt64(32, arena.allocCString(comments).address())
        seg.setInt32(40, contextLength)
        seg.setInt64(48, context.address())
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}
