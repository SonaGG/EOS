package gg.sona.eos.anticheat.client

import gg.sona.eos.anticheat.common.EosAntiCheatCommon
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class AntiCheatClientRegisterPeerOptions(
    var peerHandle: EosAntiCheatCommon.ClientHandle,
    var clientType: EosAntiCheatCommon.ClientType,
    var clientPlatform: EosAntiCheatCommon.ClientPlatform,
    var authenticationTimeoutSeconds: Int,
    var ipAddress: String?,
    var peerProductUserId: ProductUserId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 3)
        seg.setInt64(8, peerHandle.raw)
        seg.setInt32(16, clientType.value)
        seg.setInt32(20, clientPlatform.value)
        seg.setInt32(24, authenticationTimeoutSeconds)
        seg.setInt64(32, 0L) // deprecated AccountId
        seg.setInt64(40, arena.allocCString(ipAddress).address())
        seg.setInt64(48, peerProductUserId.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
        )
    }
}
