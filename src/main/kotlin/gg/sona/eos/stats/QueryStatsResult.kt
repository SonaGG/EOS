package gg.sona.eos.stats

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

public class QueryStatsResult(
    public val result: EosResult,
    public val localUserId: ProductUserId,
    public val targetUserId: ProductUserId,
)