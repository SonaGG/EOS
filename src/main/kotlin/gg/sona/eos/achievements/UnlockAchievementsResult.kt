package gg.sona.eos.achievements

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

class UnlockAchievementsResult(
    val result: EosResult,
    val userId: ProductUserId,
    val unlockedCount: Int,
)