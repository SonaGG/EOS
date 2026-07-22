/*
 * Copyright 2026 Sona Softworks LLC
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
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

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
    ): CompletableFuture<EosResult> = asyncCall(
        "EOS_Mods_InstallMod",
        ModsInstallModOptions(localUserId, mod),
    )

    public fun uninstallMod(
        localUserId: ProductUserId,
        mod: ModIdentifier,
    ): CompletableFuture<EosResult> = asyncCall(
        "EOS_Mods_UninstallMod",
        ModsUninstallModOptions(localUserId, mod),
    )

    public fun enumerateMods(
        localUserId: ProductUserId,
        type: EosModEnumerationType = EosModEnumerationType.RecentlyInstalled,
    ): CompletableFuture<EosResult> = asyncCall(
        "EOS_Mods_EnumerateMods",
        ModsEnumerateModsOptions(localUserId, type),
    )

    /**
     * All four Mods entry points are `void` C functions that deliver their result through a
     * completion delegate; none of them return an [EosResult] directly.
     */
    private fun asyncCall(function: String, options: StructWriter): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // Every EOS_Mods_*CallbackInfo begins with EOS_EResult ResultCode at offset 0.
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                function,
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Not supported: the EOS SDK has no `EOS_Mods_GetModCount` entry point. The Mods interface
     * exposes only `EOS_Mods_CopyModInfo`, which returns an `EOS_Mods_ModInfo` containing the
     * whole array and its count in one shot.
     *
     * This previously called a symbol that does not exist in the shared library, so every
     * invocation failed with [UnsatisfiedLinkError]. It now fails immediately and explains why.
     */
    @Deprecated(
        "EOS_Mods_GetModCount does not exist in the EOS SDK; use copyModInfo() and read its count.",
        level = DeprecationLevel.ERROR,
    )
    public fun getModCount(localUserId: ProductUserId): Int =
        throw UnsupportedOperationException(
            "The EOS SDK has no EOS_Mods_GetModCount function. Call EOS_Mods_CopyModInfo " +
                "(via copyModInfo) and read the mod count from the returned EOS_Mods_ModInfo."
        )

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
    ): CompletableFuture<EosResult> = asyncCall(
        "EOS_Mods_UpdateMod",
        ModsUpdateModOptions(localUserId, mod),
    )

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