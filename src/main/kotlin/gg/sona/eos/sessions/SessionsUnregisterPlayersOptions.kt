package gg.sona.eos.sessions

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class SessionsUnregisterPlayersOptions(
    var sessionName: String,
    var players: List<ProductUserId>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        val arr = arena.allocHandleArray(players.map { it.raw })
        seg.setInt64(16, arr.address())
        seg.setInt32(24, players.size)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}
