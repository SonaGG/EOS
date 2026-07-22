package gg.sona.eos.rtc

import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class RtcAdminCopyUserTokenByUserIdOptions(
    var targetUserId: Long,
    var queryId: Long,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 2)
        seg.setInt64(8, targetUserId)
        seg.setInt32(16, queryId.toInt())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}
