package gg.sona.eos.connect

import gg.sona.eos.EosResult
import gg.sona.eos.common.ContinuanceToken
import gg.sona.eos.common.ProductUserId

/** Connect login result. */
class LoginResult(
    val result: EosResult,
    val localUserId: ProductUserId,
    val continuanceToken: ContinuanceToken = ContinuanceToken(0L),
)
