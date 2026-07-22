package gg.sona.eos.presence

import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** `EOS_Presence_HasPresenceOptions`: ApiVersion@0, LocalUserId@8, TargetUserId@16. */
internal class PresenceHasPresenceOptions(
    var localUserId: EpicAccountId,
    var targetUserId: EpicAccountId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, targetUserId.raw)
        return seg
    }

    companion object {
        // EOS_PRESENCE_HASPRESENCE_API_LATEST
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}
