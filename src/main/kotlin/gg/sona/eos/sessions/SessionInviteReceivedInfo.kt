package gg.sona.eos.sessions

import gg.sona.eos.common.EpicAccountId

public class SessionInviteReceivedInfo(
    public val sessionId: String,
    public val fromUserId: EpicAccountId,
)
