package gg.sona.eos.p2p

import gg.sona.eos.internal.setInt32
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * P2P Socket ID. Socket names must be 1-32 alphanumeric characters (A-Z, a-z,
 * 0-9, '-', '_', ' ', '+', '=', '.') and may be used as a secret to gate
 * which connections are accepted.
 */
class EosP2PSocketId(val name: String) {
    init {
        require(name.length in 0..32) { "P2P socket name must be 0-32 characters" }
    }

    internal fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        val bytes = name.toByteArray(Charsets.UTF_8)
        val count = minOf(bytes.size, 32)
        for (i in 0 until count) {
            seg.set(ValueLayout.JAVA_BYTE, 4L + i, bytes[i])
        }
        seg.set(ValueLayout.JAVA_BYTE, 4L + count, 0)
        return seg
    }

    companion object {
        internal val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("ApiVersion"),
            MemoryLayout.sequenceLayout(33, ValueLayout.JAVA_BYTE).withName("SocketName"),
            MemoryLayout.paddingLayout(3),
        ).withName("EOS_P2P_SocketId")
    }
}
