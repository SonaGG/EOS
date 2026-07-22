package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId

public class PeerConnectionEstablishedInfo(
    public val localUserId: ProductUserId,
    public val remoteUserId: ProductUserId,
    public val socketId: EosP2PSocketId,
    public val type: EosConnectionEstablishedType,
    public val networkType: EosNetworkConnectionType,
)
