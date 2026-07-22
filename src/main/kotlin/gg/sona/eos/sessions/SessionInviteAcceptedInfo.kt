package gg.sona.eos.sessions

import gg.sona.eos.common.EpicAccountId

public class SessionInviteAcceptedInfo(
    public val sessionId: String,
    public val fromUserId: EpicAccountId,
    public val localUserId: EpicAccountId,
)