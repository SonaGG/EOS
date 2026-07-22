package gg.sona.eos.leaderboards

public enum class LeaderboardAggregation(val value: Int) {
    Min(0),
    Max(1),
    Sum(2),
    Latest(3);

    public companion object {
        internal fun fromValue(v: Int): LeaderboardAggregation =
            entries.firstOrNull { it.value == v } ?: Min
    }
}
