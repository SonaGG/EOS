package gg.sona.eos.achievements

class PlayerAchievement(
    val id: String,
    val progress: Double,
    val unlockTime: Long,
    val statInfo: List<PlayerStatInfo>,
    val displayName: String,
    val description: String,
    val iconUrl: String,
    val flavorText: String,
)
