package gg.sona.eos.achievements

import gg.sona.eos.common.ProductUserId

class AchievementUnlockedInfo(
    val userId: ProductUserId,
    val achievementId: String,
    val unlockTime: Long,
)
