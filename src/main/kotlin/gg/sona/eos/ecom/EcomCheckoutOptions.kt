package gg.sona.eos.ecom

import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class EcomCheckoutOptions(
    var localUserId: EpicAccountId,
    var entries: List<CheckoutEntry>,
    var preOrderPurchase: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val entrySegs = entries.map { EcomCheckoutEntry(it.offerId).writeTo(arena) }
        val arr = arena.allocate(EcomCheckoutEntry.LAYOUT, entrySegs.size.toLong())
        entrySegs.forEachIndexed { i, s ->
            val dst = arr.asSlice(
                i.toLong() * EcomCheckoutEntry.LAYOUT.byteSize(),
                EcomCheckoutEntry.LAYOUT.byteSize()
            )
            MemorySegment.copy(s, 0, dst, 0, EcomCheckoutEntry.LAYOUT.byteSize())
        }
        seg.setInt64(16, arr.address())
        seg.setInt32(24, entrySegs.size)
        seg.setInt32(28, if (preOrderPurchase) 1 else 0)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}