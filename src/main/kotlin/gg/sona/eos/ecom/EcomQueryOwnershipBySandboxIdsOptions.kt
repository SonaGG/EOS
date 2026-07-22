package gg.sona.eos.ecom

import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCStringArray
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class EcomQueryOwnershipBySandboxIdsOptions(
    var localUserId: EpicAccountId,
    var sandboxIds: List<String>,
    var catalogItemIds: List<String>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val sbArr = arena.allocCStringArray(sandboxIds)
        seg.setInt64(16, sbArr.address())
        seg.setInt32(24, sandboxIds.size)
        val catalogArr = arena.allocCStringArray(catalogItemIds)
        seg.setInt64(32, catalogArr.address())
        seg.setInt32(40, catalogItemIds.size)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}
