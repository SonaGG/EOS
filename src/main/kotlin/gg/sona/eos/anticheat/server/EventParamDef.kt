package gg.sona.eos.anticheat.server

import gg.sona.eos.anticheat.common.EosAntiCheatCommon
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** A custom event parameter definition. */
class EventParamDef(val name: String, val type: EosAntiCheatCommon.EventParamType) {
    internal fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(MemoryLayout.structLayout(ValueLayout.ADDRESS, ValueLayout.JAVA_INT))
        seg.setInt64(0, arena.allocCString(name).address())
        seg.setInt32(8, type.value)
        return seg
    }
}