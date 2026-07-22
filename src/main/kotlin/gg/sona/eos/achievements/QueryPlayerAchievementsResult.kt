package gg.sona.eos.achievements

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

class QueryPlayerAchievementsResult(
    val result: EosResult,
    val targetUserId: ProductUserId,
    val localUserId: ProductUserId,
)