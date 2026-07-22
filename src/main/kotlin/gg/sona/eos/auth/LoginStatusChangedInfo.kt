package gg.sona.eos.auth

import gg.sona.eos.common.EosLoginStatus
import gg.sona.eos.common.EpicAccountId

/** Login status change info. */
public class LoginStatusChangedInfo(
    public val localUserId: EpicAccountId,
    public val previousStatus: EosLoginStatus,
    public val currentStatus: EosLoginStatus,
)
