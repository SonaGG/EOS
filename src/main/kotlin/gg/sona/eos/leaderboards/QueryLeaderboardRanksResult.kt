package gg.sona.eos.leaderboards

import gg.sona.eos.EosResult

public class QueryLeaderboardRanksResult(
    public val result: EosResult,
    public val leaderboardId: String,
)
