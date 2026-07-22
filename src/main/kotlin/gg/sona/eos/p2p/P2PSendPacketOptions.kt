package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class P2PSendPacketOptions(
    var localUserId: ProductUserId,
    var remoteUserId: ProductUserId,
    var socketId: MemorySegment,
    var channel: Byte,
    var data: MemorySegment,
    var dataLengthBytes: Int,
    var allowDelayedDelivery: Boolean,
    var reliability: EosPacketReliability,
    var disableAutoAcceptConnection: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, remoteUserId.raw)
        seg.setInt64(24, socketId.address())
        seg.set(ValueLayout.JAVA_BYTE, 32, channel)
        seg.setInt32(36, dataLengthBytes)
        seg.setInt64(40, data.address())
        seg.setInt32(48, if (allowDelayedDelivery) 1 else 0)
        seg.setInt32(52, reliability.value)
        seg.setInt32(56, if (disableAutoAcceptConnection) 1 else 0)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
            ValueLayout.JAVA_BYTE, MemoryLayout.paddingLayout(3),
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}
