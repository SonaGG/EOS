package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Participant audio update info. */
class ParticipantUpdatedInfo(
    val localUserId: ProductUserId,
    val roomName: String,
    val participantId: ProductUserId,
    val speaking: Boolean,
    val audioStatus: EosRtcAudioStatus,
)