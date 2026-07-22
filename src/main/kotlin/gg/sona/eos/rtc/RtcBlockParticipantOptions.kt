package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class RtcBlockParticipantOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var participantId: ProductUserId,
    var blocked: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt64(24, participantId.raw)
        seg.setInt32(32, if (blocked) 1 else 0)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}
