package gg.sona.eos.friends

import gg.sona.eos.common.EpicAccountId

class FriendsUpdateInfo(
    val localUserId: EpicAccountId,
    val targetUserId: EpicAccountId,
    val previousStatus: EosFriendsStatus,
    val currentStatus: EosFriendsStatus,
)
