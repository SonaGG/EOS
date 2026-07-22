package gg.sona.eos.kws

import gg.sona.eos.common.ProductUserId

public class PermissionsUpdateInfo(
    public val localUserId: ProductUserId,
    public val kwsUserId: String,
    public val dateOfBirth: String,
    public val isMinor: Boolean,
    public val parentEmail: String,
)
