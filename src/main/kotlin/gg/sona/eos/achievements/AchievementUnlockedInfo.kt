package gg.sona.eos.achievements

import gg.sona.eos.common.ProductUserId

public class AchievementUnlockedInfo(
    public val userId: ProductUserId,
    public val achievementId: String,
    public val unlockTime: Long,
)
