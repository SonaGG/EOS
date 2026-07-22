package gg.sona.eos.anticheat.server

import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class AntiCheatServerLogGameRoundEndOptions(var winningTeamId: UInt) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(8, winningTeamId.toInt())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT,
        )
    }
}