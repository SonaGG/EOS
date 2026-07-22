package gg.sona.eos.p2p

import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout

class PacketQueueInfo(
    val incomingMaxSizeBytes: Long,
    val incomingCurrentSizeBytes: Long,
    val incomingCurrentPacketCount: Long,
    val outgoingMaxSizeBytes: Long,
    val outgoingCurrentSizeBytes: Long,
    val outgoingCurrentPacketCount: Long,
) {
    internal companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}
