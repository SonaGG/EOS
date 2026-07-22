package gg.sona.eos.custominvites

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class CustomInvitesSendCustomInviteOptions(
    var localUserId: ProductUserId,
    var recipients: List<ProductUserId>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val arr = arena.allocate(ValueLayout.JAVA_LONG, recipients.size.toLong())
        recipients.forEachIndexed { i, id ->
            arr.setAtIndex(ValueLayout.JAVA_LONG, i.toLong(), id.raw)
        }
        seg.setInt64(16, arr.address())
        seg.setInt32(24, recipients.size)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

