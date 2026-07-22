package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Participant status change info. */
public class ParticipantStatusChangedInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val participantId: ProductUserId,
    public val status: EosRtcParticipantStatus,
    public val inBlocklist: Boolean,
)