package gg.sona.eos.connect

import gg.sona.eos.common.EosLoginStatus
import gg.sona.eos.common.ProductUserId

/** Connect login status change info. */
public class LoginStatusChangedInfo(
    public val localUserId: ProductUserId,
    public val previousStatus: EosLoginStatus,
    public val currentStatus: EosLoginStatus,
)
