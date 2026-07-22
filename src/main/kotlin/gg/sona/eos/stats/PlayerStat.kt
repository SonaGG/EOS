package gg.sona.eos.stats

/** A cached stat for a user. */
class PlayerStat(
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val value: Int,
)