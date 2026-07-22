package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class AntiCheatServerRegisterEventOptions(
    var eventId: UInt,
    var eventName: String,
    var eventType: EosAntiCheatCommon.EventType,
    var paramDefsCount: Int,
    var paramDefs: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(8, eventId.toInt())
        seg.setInt64(16, arena.allocCString(eventName).address())
        seg.setInt32(24, eventType.value)
        seg.setInt32(28, paramDefsCount)
        seg.setInt64(32, paramDefs.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}