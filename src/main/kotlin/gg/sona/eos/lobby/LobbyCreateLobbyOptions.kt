package gg.sona.eos.lobby

import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class LobbyCreateLobbyOptions(
    var localUserId: EpicAccountId?,
    var maxPlayers: Int,
    var presenceEnabled: Boolean,
    var allowInvites: Boolean,
    var lobbyId: String,
    var rtName: String?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId?.raw ?: 0L)
        seg.setInt32(16, maxPlayers)
        seg.setInt32(20, if (presenceEnabled) 1 else 0)
        seg.setInt32(24, if (allowInvites) 1 else 0)
        seg.setInt64(32, arena.allocCString(lobbyId).address())
        seg.setInt64(40, arena.allocCString(rtName).address())
        return seg
    }

    companion object {
        const val API_LATEST = 5
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS, ValueLayout.ADDRESS,
        )
    }
}
