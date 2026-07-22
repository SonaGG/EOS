package gg.sona.eos.integratedplatform

import gg.sona.eos.common.ProductUserId

class UserLoginStatusChangedInfo(
    val localUserId: ProductUserId,
    val previousPlatform: String,
    val currentPlatform: String,
    val previousStatus: EosIntegratedPlatformLoginStatus,
    val currentStatus: EosIntegratedPlatformLoginStatus,
)