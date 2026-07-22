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

internal class AntiCheatServerLogGameRoundStartOptions(
    var sessionIdentifier: String?,
    var levelName: String?,
    var modeName: String?,
    var roundTimeSeconds: UInt,
    var competitionType: EosAntiCheatCommon.GameRoundCompetitionType,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 2)
        seg.setInt64(8, arena.allocCString(sessionIdentifier).address())
        seg.setInt64(16, arena.allocCString(levelName).address())
        seg.setInt64(24, arena.allocCString(modeName).address())
        seg.setInt32(32, roundTimeSeconds.toInt())
        seg.setInt32(36, competitionType.value)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}
