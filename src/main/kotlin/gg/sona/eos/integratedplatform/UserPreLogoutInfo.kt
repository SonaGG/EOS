package gg.sona.eos.integratedplatform

import gg.sona.eos.common.ProductUserId

public class UserPreLogoutInfo(
    public val localUserId: ProductUserId,
    public val platform: String,
)
