package gg.sona.eos.auth

import gg.sona.eos.EosResult
import gg.sona.eos.common.EpicAccountId

/** Login result. */
class LoginResult(
    val result: EosResult,
    val localUserId: EpicAccountId,
)