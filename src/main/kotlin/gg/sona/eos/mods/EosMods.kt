/*
 * Copyright 2024 sona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gg.sona.eos.mods

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Mods interface. Manages mod installation, updates, and enumeration.
 */
public class EosMods internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetModsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    public fun installMod(
        localUserId: ProductUserId,
        mod: ModIdentifier,
    ): EosResult = withCallArena { arena ->
        val options = ModsInstallModOptions(localUserId, mod)
        EosResult.fromValue(
            Native.invoke(
                "EOS_Mods_InstallMod",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun uninstallMod(
        localUserId: ProductUserId,
        mod: ModIdentifier,
    ): EosResult = withCallArena { arena ->
        val options = ModsInstallModOptions(localUserId, mod)
        EosResult.fromValue(
            Native.invoke(
                "EOS_Mods_UninstallMod",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun enumerateMods(
        localUserId: ProductUserId,
        type: EosModEnumerationType = EosModEnumerationType.RecentlyInstalled,
    ): EosResult = withCallArena { arena ->
        val options = ModsEnumerateModsOptions(localUserId, type)
        EosResult.fromValue(
            Native.invoke(
                "EOS_Mods_EnumerateMods",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun getModCount(localUserId: ProductUserId): Int {
        val options = ModsGetModCountOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Mods_GetModCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    public fun copyModInfoByIndex(
        localUserId: ProductUserId,
        index: Int,
    ): ModInfo? = withCallArena { arena ->
        val outPtr = arena.allocate(ValueLayout.ADDRESS)
        val options = ModsCopyModInfoByIndexOptions(localUserId, index)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_Mods_CopyModInfo",
                listOf(handle(), options.writeTo(arena), outPtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result != EosResult.Success) return@withCallArena null
        val seg = outPtr.get(ValueLayout.ADDRESS, 0)
        if (seg.address() == 0L) return@withCallArena null
        val info = readModInfo(seg)
        Native.downcall(
            "EOS_Mods_ModInfo_Release",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        ).invokeExact(seg)
        info
    }

    public fun updateMod(
        localUserId: ProductUserId,
        mod: ModIdentifier,
    ): EosResult = withCallArena { arena ->
        val options = ModsUpdateModOptions(localUserId, mod)
        EosResult.fromValue(
            Native.invoke(
                "EOS_Mods_UpdateMod",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    private fun readString(seg: MemorySegment, offset: Long): String {
        val ptr = seg.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return ""
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }

    private fun readModInfo(seg: MemorySegment): ModInfo = ModInfo(
        type = readString(seg, 8),
        id = readString(seg, 16),
        name = readString(seg, 24),
        version = readString(seg, 32),
        enabled = seg.getInt32(40) != 0,
    )
}

public enum class EosModEnumerationType(val value: Int) {
    RecentlyInstalled(0);

    public companion object {
        internal fun fromValue(v: Int): EosModEnumerationType = entries.firstOrNull { it.value == v } ?: RecentlyInstalled
    }
}

public class ModIdentifier(public val type: String, public val id: String)

public class ModInfo(
    public val type: String,
    public val id: String,
    public val name: String,
    public val version: String,
    public val enabled: Boolean,
)

internal class ModsModIdentifier(public val mod: ModIdentifier) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(mod.type).address())
        seg.setInt64(16, arena.allocCString(mod.id).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.ADDRESS,
        )
    }
}

internal class ModsInstallModOptions(
    var localUserId: ProductUserId,
    var mod: ModIdentifier,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val modSeg = ModsModIdentifier(mod).writeTo(arena)
        seg.setInt64(16, modSeg.address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class ModsEnumerateModsOptions(
    var localUserId: ProductUserId,
    var type: EosModEnumerationType,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, type.value)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class ModsGetModCountOptions(var localUserId: ProductUserId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}

internal class ModsCopyModInfoByIndexOptions(
    var localUserId: ProductUserId,
    var index: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, index)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class ModsUpdateModOptions(
    var localUserId: ProductUserId,
    var mod: ModIdentifier,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val modSeg = ModsModIdentifier(mod).writeTo(arena)
        seg.setInt64(16, modSeg.address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}
