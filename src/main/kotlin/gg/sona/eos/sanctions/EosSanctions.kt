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
package gg.sona.eos.sanctions

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.*
import java.util.concurrent.CompletableFuture

/**
 * Sanctions interface. Query player sanctions and submit appeals.
 */
public class EosSanctions internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetSanctionsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    public fun queryActivePlayerSanctions(targetUserId: ProductUserId): CompletableFuture<EosResult> =
        asyncCall(
            "EOS_Sanctions_QueryActivePlayerSanctions",
            SanctionsQueryActivePlayerSanctionsOptions(targetUserId),
        )

    /**
     * Both Sanctions entry points are `void` C functions that deliver their result through a
     * completion delegate; neither returns an [EosResult] directly.
     */
    private fun asyncCall(function: String, options: StructWriter): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // Every EOS_Sanctions_*CallbackInfo begins with EOS_EResult ResultCode at offset 0.
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

    public fun getPlayerSanctionCount(targetUserId: ProductUserId): Int {
        val options = SanctionsGetPlayerSanctionCountOptions(targetUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Sanctions_GetPlayerSanctionCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    public fun copyPlayerSanctionByIndex(targetUserId: ProductUserId, index: Int): Sanction? =
        withCallArena { arena ->
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val options = SanctionsCopyPlayerSanctionByIndexOptions(targetUserId, index)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Sanctions_CopyPlayerSanctionByIndex",
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val sanction = readSanction(seg)
            Native.downcall(
                "EOS_Sanctions_PlayerSanction_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            sanction
        }

    public fun createPlayerSanctionAppeal(
        targetUserId: ProductUserId,
        sanctionId: String,
        reason: String,
    ): CompletableFuture<EosResult> = asyncCall(
        "EOS_Sanctions_CreatePlayerSanctionAppeal",
        SanctionsCreatePlayerSanctionAppealOptions(targetUserId, sanctionId, reason),
    )

    private fun readString(seg: MemorySegment, offset: Long): String {
        val ptr = seg.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return ""
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }

    private fun readSanction(seg: MemorySegment): Sanction = Sanction(
        sanctionId = readString(seg, 8),
        timePlaced = readString(seg, 16),
        action = readString(seg, 24),
    )
}
