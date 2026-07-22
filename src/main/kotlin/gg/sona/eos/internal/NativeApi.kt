package gg.sona.eos.internal

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** Allocates a fresh [Arena] confined to the current thread for one call. */
@PublishedApi
internal inline fun <T> withCallArena(block: (Arena) -> T): T {
    return Arena.ofConfined().use(block)
}

/** Alias for [withCallArena] used by call sites that read better with this name. */
@PublishedApi
internal inline fun <T> withArena(block: (Arena) -> T): T = withCallArena(block)

/**
 * Run a block with the given struct value written into a fresh native arena.
 * The struct's [StructWriter.writeTo] is invoked once with the segment and
 * arena. The block must not retain the segment past its execution.
 */
@PublishedApi
internal inline fun <T> withStruct(value: StructWriter, block: (MemorySegment, Arena) -> T): T {
    return withCallArena { arena ->
        val segment = value.writeTo(arena)
        block(segment, arena)
    }
}

/** Allocates a null-terminated UTF-8 string in the arena, or [MemorySegment.NULL] if [value] is null. */
fun Arena.allocCString(value: String?): MemorySegment =
    if (value == null) MemorySegment.NULL else allocateFrom(value)

/** Allocates a UTF-8 string array in the arena; null entries become [MemorySegment.NULL]. */
fun Arena.allocCStringArray(values: List<String?>): MemorySegment {
    val arr = allocate(ValueLayout.ADDRESS, values.size.toLong())
    values.forEachIndexed { i, v ->
        arr.setAtIndex(ValueLayout.ADDRESS, i.toLong(), allocCString(v))
    }
    return arr
}

/** Allocates an array of opaque handle Longs (eos opaque pointers). */
fun Arena.allocHandleArray(values: List<Long>): MemorySegment {
    val arr = allocate(ValueLayout.JAVA_LONG, values.size.toLong())
    values.forEachIndexed { i, v ->
        arr.setAtIndex(ValueLayout.JAVA_LONG, i.toLong(), v)
    }
    return arr
}

/** Reads a null-terminated C string at the given offset, returning null if the pointer is null. */
fun MemorySegment.getCStringAt(offset: Long): String? {
    val ptr = get(ValueLayout.ADDRESS, offset)
    if (ptr.address() == 0L) return null
    return ptr.reinterpret(Long.MAX_VALUE).getString(0)
}

/** Reads a fixed-size byte array (e.g. SocketName[33]) as a UTF-8 string up to the first NUL. */
fun MemorySegment.getFixedCString(offset: Long, maxBytes: Int): String {
    val bytes = ByteArray(maxBytes)
    MemorySegment.copy(this, ValueLayout.JAVA_BYTE, offset, bytes, 0, maxBytes)
    val nul = bytes.indexOf(0.toByte())
    val len = if (nul < 0) maxBytes else nul
    return String(bytes, 0, len, Charsets.UTF_8)
}