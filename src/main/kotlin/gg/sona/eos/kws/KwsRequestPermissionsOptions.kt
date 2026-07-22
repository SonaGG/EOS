package gg.sona.eos.kws

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class KwsRequestPermissionsOptions(
    var localUserId: ProductUserId,
    var permissionKeys: List<String>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, permissionKeys.size)
        val arr = arena.allocate(ValueLayout.ADDRESS, permissionKeys.size.toLong())
        permissionKeys.forEachIndexed { i, k ->
            arr.setAtIndex(ValueLayout.ADDRESS, i.toLong(), arena.allocCString(k))
        }
        seg.setInt64(24, arr.address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}