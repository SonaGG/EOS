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

internal class EcomQueryEntitlementsOptions(
    var localUserId: EpicAccountId?,
    var entitlementNames: List<String>?,
    var includeRedeemed: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        val en = entitlementNames
        if (en != null) {
            val arr = arena.allocCStringArray(en)
            seg.setInt64(16, arr.address())
            seg.setInt32(24, en.size)
        } else {
            seg.setInt64(16, 0L)
            seg.setInt32(24, 0)
        }
        seg.setInt32(28, if (includeRedeemed) 1 else 0)
        return seg
    }

    companion object {
        const val API_LATEST = 3
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}