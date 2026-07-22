package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

class RtcDataParticipantUpdatedInfo(
    val localUserId: ProductUserId,
    val roomName: String,
    val participantId: ProductUserId,
    val status: EosRtcDataStatus,
)