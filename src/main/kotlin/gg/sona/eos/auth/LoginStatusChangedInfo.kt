package gg.sona.eos.auth

import gg.sona.eos.common.EosLoginStatus
import gg.sona.eos.common.EpicAccountId

/** Login status change info. */
class LoginStatusChangedInfo(
    val localUserId: EpicAccountId,
    val previousStatus: EosLoginStatus,
    val currentStatus: EosLoginStatus,
)
