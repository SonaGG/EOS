package gg.sona.eos.kws

import gg.sona.eos.common.ProductUserId

class PermissionsUpdateInfo(
    val localUserId: ProductUserId,
    val kwsUserId: String,
    val dateOfBirth: String,
    val isMinor: Boolean,
    val parentEmail: String,
)
