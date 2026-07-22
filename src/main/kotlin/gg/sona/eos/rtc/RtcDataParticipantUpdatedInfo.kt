package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

public class RtcDataParticipantUpdatedInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val participantId: ProductUserId,
    public val status: EosRtcDataStatus,
)