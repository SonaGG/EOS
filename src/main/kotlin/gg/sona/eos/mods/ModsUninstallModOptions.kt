package gg.sona.eos.mods

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** `EOS_Mods_UninstallModOptions`: ApiVersion@0, LocalUserId@8, Mod@16 (no `bRemoveAfterExit`). */
internal class ModsUninstallModOptions(
    var localUserId: ProductUserId,
    var mod: ModIdentifier,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, ModsModIdentifier(mod).writeTo(arena).address())
        return seg
    }

    companion object {
        // EOS_MODS_UNINSTALLMOD_API_LATEST
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}