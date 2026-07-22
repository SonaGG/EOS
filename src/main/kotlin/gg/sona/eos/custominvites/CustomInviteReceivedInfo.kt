package gg.sona.eos.custominvites

import gg.sona.eos.common.ProductUserId

class CustomInviteReceivedInfo(
    val inviteId: String,
    val fromUserId: ProductUserId,
    val payload: String,
)