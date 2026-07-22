package gg.sona.eos.internal

import java.lang.foreign.MemorySegment

/** A handle to a registered callback. */
internal class StubHandle internal constructor(
    @JvmField internal val id: Long,
    @JvmField internal val segment: MemorySegment,
)
