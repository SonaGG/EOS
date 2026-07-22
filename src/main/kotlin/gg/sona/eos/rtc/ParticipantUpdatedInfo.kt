package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

/** Participant audio update info. */
public class ParticipantUpdatedInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val participantId: ProductUserId,
    public val speaking: Boolean,
    public val audioStatus: EosRtcAudioStatus,
)