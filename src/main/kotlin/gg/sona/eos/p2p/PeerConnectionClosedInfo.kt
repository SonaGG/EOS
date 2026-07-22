package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId

public class PeerConnectionClosedInfo(
    public val localUserId: ProductUserId,
    public val remoteUserId: ProductUserId,
    public val socketId: EosP2PSocketId,
    public val reason: EosConnectionClosedReason,
)
