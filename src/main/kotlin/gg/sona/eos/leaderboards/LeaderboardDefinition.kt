package gg.sona.eos.leaderboards

public class LeaderboardDefinition(
    public val leaderboardId: String,
    public val statName: String,
    public val aggregation: LeaderboardAggregation,
    public val startTime: Long,
    public val endTime: Long,
)