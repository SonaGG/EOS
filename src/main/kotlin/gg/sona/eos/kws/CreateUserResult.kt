package gg.sona.eos.kws

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

class CreateUserResult(
    val result: EosResult,
    val localUserId: ProductUserId,
    val kwsUserId: String,
    val isMinor: Boolean,
)