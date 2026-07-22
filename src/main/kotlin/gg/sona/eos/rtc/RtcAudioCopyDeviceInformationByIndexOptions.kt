package gg.sona.eos.rtc

import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** Shared by EOS_RTCAudio_Copy{Input,Output}DeviceInformationByIndexOptions. */
internal class RtcAudioCopyDeviceInformationByIndexOptions(
    var deviceIndex: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(4, deviceIndex)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}
