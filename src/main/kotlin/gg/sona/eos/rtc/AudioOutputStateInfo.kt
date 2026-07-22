package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Local audio output state change info. */
public class AudioOutputStateInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val status: EosRtcAudioOutputStatus,
)