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
public class EosP2PSocketId(public val name: String) {
    init {
        require(name.length in 0..32) { "P2P socket name must be 0-32 characters" }
    }

    internal fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        val bytes = name.toByteArray(Charsets.UTF_8)
        for (i in 0 until 32) {
            seg.set(ValueLayout.JAVA_BYTE, 4L + i, if (i < bytes.size) bytes[i] else 0)
        }
        return seg
    }

    public companion object {
        internal val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(0), ValueLayout.JAVA_BYTE
        ).withByteAlignment(1)
            // The fixed-size char[33] is laid out as 33 bytes
            .withName("EOS_P2P_SocketId")
    }
}
