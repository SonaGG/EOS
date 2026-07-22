package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId

class PeerConnectionRequestInfo(
    val localUserId: ProductUserId,
    val remoteUserId: ProductUserId,
    val socketId: EosP2PSocketId,
)