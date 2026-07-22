package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId

public class IncomingPacketQueueFullInfo(
    public val maxSizeBytes: Long,
    public val currentSizeBytes: Long,
    public val localUserId: ProductUserId,
    public val channel: Int,
    public val packetSizeBytes: Long,
)