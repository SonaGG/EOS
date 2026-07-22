package gg.sona.eos.leaderboards

import gg.sona.eos.common.ProductUserId

public class LeaderboardRecord(
    public val userId: ProductUserId,
    public val rank: UInt,
    public val score: Int,
    public val userDisplayName: String,
)