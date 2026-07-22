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

internal class RtcJoinRoomOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var clientBaseUrl: String,
    var participantToken: String,
    var participantId: ProductUserId?,
    var flags: Int,
    var manualAudioInput: Boolean,
    var manualAudioOutput: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1) // API version
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt64(24, arena.allocCString(clientBaseUrl).address())
        seg.setInt64(32, arena.allocCString(participantToken).address())
        seg.setInt64(40, participantId?.raw ?: 0L)
        seg.setInt32(48, flags)
        seg.setInt32(52, if (manualAudioInput) 1 else 0)
        seg.setInt32(56, if (manualAudioOutput) 1 else 0)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
            ValueLayout.JAVA_INT,
        )
    }
}

