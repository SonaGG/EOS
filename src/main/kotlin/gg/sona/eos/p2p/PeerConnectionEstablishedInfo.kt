package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId

class PeerConnectionEstablishedInfo(
    val localUserId: ProductUserId,
    val remoteUserId: ProductUserId,
    val socketId: EosP2PSocketId,
    val type: EosConnectionEstablishedType,
    val networkType: EosNetworkConnectionType,
)
