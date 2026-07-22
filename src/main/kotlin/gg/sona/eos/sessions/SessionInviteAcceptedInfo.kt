package gg.sona.eos.sessions

import gg.sona.eos.common.EpicAccountId

class SessionInviteAcceptedInfo(
    val sessionId: String,
    val fromUserId: EpicAccountId,
    val localUserId: EpicAccountId,
)