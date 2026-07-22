package gg.sona.eos.presence

import gg.sona.eos.common.EpicAccountId

public class PresenceChangedInfo(
    public val localUserId: EpicAccountId,
    public val presenceUserId: EpicAccountId,
)
