package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId

class P2PReceivedPacket(
    val remoteUserId: ProductUserId,
    val socketId: EosP2PSocketId,
    val channel: Int,
    val data: ByteArray,
)