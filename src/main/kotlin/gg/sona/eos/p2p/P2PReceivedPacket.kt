package gg.sona.eos.p2p

import gg.sona.eos.common.ProductUserId

public class P2PReceivedPacket(
    public val remoteUserId: ProductUserId,
    public val socketId: EosP2PSocketId,
    public val channel: Int,
    public val data: ByteArray,
)