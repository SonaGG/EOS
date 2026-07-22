package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Participant status change info. */
class ParticipantStatusChangedInfo(
    val localUserId: ProductUserId,
    val roomName: String,
    val participantId: ProductUserId,
    val status: EosRtcParticipantStatus,
    val inBlocklist: Boolean,
)