package gg.sona.eos.internal

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

@PublishedApi
internal interface StructWriterInternal {
    fun writeTo(arena: Arena): MemorySegment
}
