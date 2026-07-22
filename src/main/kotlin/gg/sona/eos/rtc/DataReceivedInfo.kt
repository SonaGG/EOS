package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

public class DataReceivedInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val data: ByteArray,
    public val sender: ProductUserId,
)