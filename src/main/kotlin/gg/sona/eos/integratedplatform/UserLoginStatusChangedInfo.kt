package gg.sona.eos.integratedplatform

import gg.sona.eos.common.ProductUserId

public class UserLoginStatusChangedInfo(
    public val localUserId: ProductUserId,
    public val previousPlatform: String,
    public val currentPlatform: String,
    public val previousStatus: EosIntegratedPlatformLoginStatus,
    public val currentStatus: EosIntegratedPlatformLoginStatus,
)