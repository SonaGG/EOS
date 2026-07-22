package gg.sona.eos.friends

import gg.sona.eos.common.EpicAccountId

public class FriendsUpdateInfo(
    public val localUserId: EpicAccountId,
    public val targetUserId: EpicAccountId,
    public val previousStatus: EosFriendsStatus,
    public val currentStatus: EosFriendsStatus,
)
