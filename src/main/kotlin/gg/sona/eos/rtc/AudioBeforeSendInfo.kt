package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Outgoing audio, delivered just before encoding. */
public class AudioBeforeSendInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val buffer: AudioFrames,
)