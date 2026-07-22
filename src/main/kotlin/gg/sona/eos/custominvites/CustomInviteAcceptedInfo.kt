package gg.sona.eos.custominvites

import gg.sona.eos.common.ProductUserId

public class CustomInviteAcceptedInfo(
    public val inviteId: String,
    public val fromUserId: ProductUserId,
    public val payload: String,
)
