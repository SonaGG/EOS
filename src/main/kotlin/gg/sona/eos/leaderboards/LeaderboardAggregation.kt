package gg.sona.eos.leaderboards

enum class LeaderboardAggregation(val value: Int) {
    Min(0),
    Max(1),
    Sum(2),
    Latest(3);

    companion object {
        internal fun fromValue(v: Int): LeaderboardAggregation =
            entries.firstOrNull { it.value == v } ?: Min
    }
}
