package gg.sona.eos.ecom

import gg.sona.eos.EosResult
import gg.sona.eos.common.EpicAccountId

class CheckoutResult(
    val result: EosResult,
    val localUserId: EpicAccountId,
    val transactionId: String,
)
