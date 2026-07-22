package gg.sona.eos.achievements

import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout

/** A single player stat-info record within a player achievement. */
class PlayerStatInfo(val name: String, val currentValue: Int, val thresholdValue: Int) {
    internal companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}