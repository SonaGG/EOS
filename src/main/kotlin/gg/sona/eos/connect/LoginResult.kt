package gg.sona.eos.connect

import gg.sona.eos.EosResult
import gg.sona.eos.common.ContinuanceToken
import gg.sona.eos.common.ProductUserId

/** Connect login result. */
public class LoginResult(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val continuanceToken: ContinuanceToken = ContinuanceToken(0L),
)
