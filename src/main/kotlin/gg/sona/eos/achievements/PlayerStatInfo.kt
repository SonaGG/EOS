package gg.sona.eos.achievements

import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout

/** A single player stat-info record within a player achievement. */
public class PlayerStatInfo(public val name: String, public val currentValue: Int, public val thresholdValue: Int) {
    internal companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}