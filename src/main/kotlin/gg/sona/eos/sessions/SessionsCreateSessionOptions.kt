package gg.sona.eos.sessions

import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class SessionsCreateSessionOptions(
    var sessionName: String,
    var bucketId: String,
    var maxPlayers: Int,
    var localUserId: EpicAccountId?,
    var presenceEnabled: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        seg.setInt64(16, arena.allocCString(bucketId).address())
        seg.setInt32(24, maxPlayers)
        seg.setInt64(32, localUserId?.raw ?: 0L)
        seg.setInt32(40, if (presenceEnabled) 1 else 0)
        return seg
    }

    companion object {
        const val API_LATEST = 5
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}
