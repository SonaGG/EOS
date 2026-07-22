package gg.sona.eos.p2p

import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout

public class PacketQueueInfo(
    public val incomingMaxSizeBytes: Long,
    public val incomingCurrentSizeBytes: Long,
    public val incomingCurrentPacketCount: Long,
    public val outgoingMaxSizeBytes: Long,
    public val outgoingCurrentSizeBytes: Long,
    public val outgoingCurrentPacketCount: Long,
) {
    internal companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}
