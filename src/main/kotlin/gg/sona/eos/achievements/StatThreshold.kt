package gg.sona.eos.achievements

import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout

/** A single stat threshold within an achievement definition. */
public class StatThreshold(public val name: String, public val threshold: Int) {
    internal companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}