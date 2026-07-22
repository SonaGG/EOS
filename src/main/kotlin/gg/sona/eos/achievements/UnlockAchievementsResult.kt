package gg.sona.eos.achievements

import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId

public class UnlockAchievementsResult(
    public val result: EosResult,
    public val userId: ProductUserId,
    public val unlockedCount: Int,
)