package gg.sona.eos.userinfo

import gg.sona.eos.common.EosExternalAccountType
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class UserInfoQueryUserInfoByExternalAccountOptions(
    var localUserId: EpicAccountId,
    var externalAccountType: EosExternalAccountType,
    var externalAccountId: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, externalAccountType.ordinal)
        seg.setInt64(24, arena.allocCString(externalAccountId).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}
