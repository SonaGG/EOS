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

internal class LobbySendInviteOptions(
    var lobbyId: String,
    var localUserId: EpicAccountId?,
    var targetUserId: EpicAccountId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(lobbyId).address())
        seg.setInt64(16, localUserId?.raw ?: 0L)
        seg.setInt64(24, targetUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}