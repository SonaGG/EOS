package gg.sona.eos.ecom

import gg.sona.eos.EosResult
import gg.sona.eos.common.EpicAccountId

class QueryEntitlementTokenResult(
    val result: EosResult,
    val localUserId: EpicAccountId,
    val token: String,
)