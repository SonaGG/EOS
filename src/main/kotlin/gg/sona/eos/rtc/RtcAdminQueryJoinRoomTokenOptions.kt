package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCStringArray
import gg.sona.eos.internal.allocHandleArray
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class RtcAdminQueryJoinRoomTokenOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var targetUserIds: List<Long>,
    var targetUserIpAddresses: List<String?>?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 2)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        val userIdsArr = arena.allocHandleArray(targetUserIds)
        seg.setInt64(24, userIdsArr.address())
        seg.setInt32(32, targetUserIds.size)
        val ipArr = targetUserIpAddresses?.let { arena.allocCStringArray(it) } ?: MemorySegment.NULL
        seg.setInt64(40, ipArr.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}
