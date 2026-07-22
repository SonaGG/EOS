package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class P2PGetNextReceivedPacketSizeOptions(
    var localUserId: ProductUserId,
    var channel: Int?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 2)
        seg.setInt64(8, localUserId.raw)
        if (channel != null) {
            val chValue: Int = channel!!
            val ch = arena.allocate(ValueLayout.JAVA_BYTE)
            ch.set(ValueLayout.JAVA_BYTE, 0, chValue.toByte())
            seg.setInt64(16, ch.address())
        } else {
            seg.setInt64(16, 0L)
        }
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}