package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId

class PeerConnectionClosedInfo(
    val localUserId: ProductUserId,
    val remoteUserId: ProductUserId,
    val socketId: EosP2PSocketId,
    val reason: EosConnectionClosedReason,
)
