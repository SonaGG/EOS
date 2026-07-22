package gg.sona.eos.leaderboards

import gg.sona.eos.common.ProductUserId

class LeaderboardRecord(
    val userId: ProductUserId,
    val rank: UInt,
    val score: Int,
    val userDisplayName: String,
)