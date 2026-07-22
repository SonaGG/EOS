package gg.sona.eos.anticheat.server

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class AntiCheatServerBeginSessionOptions(
    var registerTimeoutSeconds: Int,
    var serverName: String?,
    var enableGameplayData: Boolean,
    var localUserId: ProductUserId?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt32(8, registerTimeoutSeconds)
        seg.setInt64(16, arena.allocCString(serverName).address())
        seg.setInt32(24, if (enableGameplayData) 1 else 0)
        seg.setInt64(32, localUserId?.raw ?: 0L)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG,
        )
    }
}
