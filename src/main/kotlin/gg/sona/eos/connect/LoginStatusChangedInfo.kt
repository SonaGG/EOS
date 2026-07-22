package gg.sona.eos.connect

import gg.sona.eos.common.EosLoginStatus
import gg.sona.eos.common.ProductUserId

/** Connect login status change info. */
class LoginStatusChangedInfo(
    val localUserId: ProductUserId,
    val previousStatus: EosLoginStatus,
    val currentStatus: EosLoginStatus,
)
