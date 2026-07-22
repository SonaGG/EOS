package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class RtcAudioUpdateParticipantVolumeOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var participantId: ProductUserId?,
    var volume: Float,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt64(24, participantId?.raw ?: 0L)
        seg.setFloat(32, volume)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_FLOAT,
        )
    }
}
