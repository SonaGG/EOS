package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Local audio input state change info. */
class AudioInputStateInfo(
    val localUserId: ProductUserId,
    val roomName: String,
    val status: EosRtcAudioInputStatus,
)
