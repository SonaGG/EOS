package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Outgoing audio, delivered just before encoding. */
class AudioBeforeSendInfo(
    val localUserId: ProductUserId,
    val roomName: String,
    val buffer: AudioFrames,
)