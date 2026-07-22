package gg.sona.eos.metrics

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class MetricsBeginPlayerSessionOptions(
    var localUserId: ProductUserId?,
    var displayName: String?,
    var controllerType: EosMetricsControllerType,
    var serverIp: String?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        seg.setInt64(16, arena.allocCString(displayName).address())
        seg.setInt64(24, arena.allocCString(serverIp).address())
        seg.setInt32(32, controllerType.value)
        return seg
    }

    companion object {
        const val API_LATEST = 3
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
        )
    }
}