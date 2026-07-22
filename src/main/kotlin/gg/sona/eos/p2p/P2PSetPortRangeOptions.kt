package gg.sona.eos.p2p

import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class P2PSetPortRangeOptions(
    var port: Short,
    var maxAdditionalPorts: Short,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.set(ValueLayout.JAVA_SHORT, 4, port)
        seg.set(ValueLayout.JAVA_SHORT, 6, maxAdditionalPorts)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_SHORT, ValueLayout.JAVA_SHORT,
        )
    }
}