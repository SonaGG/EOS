package gg.sona.eos.rtc

import gg.sona.eos.common.ProductUserId

public class RtcAdminUserToken(
    public val productUserId: ProductUserId,
    public val token: String,
)
