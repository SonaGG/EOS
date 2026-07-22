package gg.sona.eos.ecom

import gg.sona.eos.EosResult
import gg.sona.eos.common.EpicAccountId

public class QueryOwnershipTokenResult(
    public val result: EosResult,
    public val localUserId: EpicAccountId,
    public val token: String,
)