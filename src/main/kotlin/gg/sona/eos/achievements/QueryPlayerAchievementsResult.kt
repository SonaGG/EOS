package gg.sona.eos.achievements

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

public class QueryPlayerAchievementsResult(
    public val result: EosResult,
    public val targetUserId: ProductUserId,
    public val localUserId: ProductUserId,
)