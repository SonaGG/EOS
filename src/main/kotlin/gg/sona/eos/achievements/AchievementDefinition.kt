package gg.sona.eos.achievements

class AchievementDefinition(
    val id: String,
    val unlockedDisplayName: String,
    val unlockedDescription: String,
    val lockedDisplayName: String,
    val lockedDescription: String,
    val flavorText: String,
    val unlockedIconUrl: String,
    val lockedIconUrl: String,
    val isHidden: Boolean,
    val statThresholds: List<StatThreshold>,
)
