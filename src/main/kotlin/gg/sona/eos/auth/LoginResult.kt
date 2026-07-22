package gg.sona.eos.auth

import gg.sona.eos.EosResult
import gg.sona.eos.common.EpicAccountId

/** Login result. */
public class LoginResult(
    public val result: EosResult,
    public val localUserId: EpicAccountId,
)