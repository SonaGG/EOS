package gg.sona.eos.stats

/** A cached stat for a user. */
public class PlayerStat(
    public val name: String,
    public val startTime: Long,
    public val endTime: Long,
    public val value: Int,
)