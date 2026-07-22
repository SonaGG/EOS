package gg.sona.eos.leaderboards

class LeaderboardDefinition(
    val leaderboardId: String,
    val statName: String,
    val aggregation: LeaderboardAggregation,
    val startTime: Long,
    val endTime: Long,
)