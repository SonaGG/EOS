package gg.sona.eos.achievements

public class AchievementDefinition(
    public val id: String,
    public val unlockedDisplayName: String,
    public val unlockedDescription: String,
    public val lockedDisplayName: String,
    public val lockedDescription: String,
    public val flavorText: String,
    public val unlockedIconUrl: String,
    public val lockedIconUrl: String,
    public val isHidden: Boolean,
    public val statThresholds: List<StatThreshold>,
)
