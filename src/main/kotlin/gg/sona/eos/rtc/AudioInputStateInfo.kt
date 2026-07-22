package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Local audio input state change info. */
public class AudioInputStateInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val status: EosRtcAudioInputStatus,
)
