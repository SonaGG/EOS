package gg.sona.eos.kws

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

public class CreateUserResult(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val kwsUserId: String,
    public val isMinor: Boolean,
)