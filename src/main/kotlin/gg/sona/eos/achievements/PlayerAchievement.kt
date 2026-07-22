package gg.sona.eos.achievements

public class PlayerAchievement(
    public val id: String,
    public val progress: Double,
    public val unlockTime: Long,
    public val statInfo: List<PlayerStatInfo>,
    public val displayName: String,
    public val description: String,
    public val iconUrl: String,
    public val flavorText: String,
)
