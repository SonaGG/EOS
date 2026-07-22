package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId

class IncomingPacketQueueFullInfo(
    val maxSizeBytes: Long,
    val currentSizeBytes: Long,
    val localUserId: ProductUserId,
    val channel: Int,
    val packetSizeBytes: Long,
)