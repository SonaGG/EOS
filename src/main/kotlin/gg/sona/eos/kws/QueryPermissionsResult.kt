package gg.sona.eos.kws

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

class QueryPermissionsResult(
    val result: EosResult,
    val localUserId: ProductUserId,
    val kwsUserId: String,
    val dateOfBirth: String,
    val isMinor: Boolean,
    val parentEmail: String,
)