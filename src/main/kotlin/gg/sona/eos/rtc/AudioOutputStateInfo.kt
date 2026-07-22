package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Local audio output state change info. */
class AudioOutputStateInfo(
    val localUserId: ProductUserId,
    val roomName: String,
    val status: EosRtcAudioOutputStatus,
)