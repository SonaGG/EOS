package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

class DataReceivedInfo(
    val localUserId: ProductUserId,
    val roomName: String,
    val data: ByteArray,
    val sender: ProductUserId,
)