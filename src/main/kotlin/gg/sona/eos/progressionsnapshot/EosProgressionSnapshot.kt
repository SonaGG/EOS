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
package gg.sona.eos.progressionsnapshot

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Progression Snapshot interface. Bulk-submit player progression data.
 *
 * The typical flow:
 *  1. [beginSnapshot] to start a session
 *  2. [addProgression] for each stat/key/value
 *  3. [submitSnapshot] to upload
 *  4. [endSnapshot] to clean up
 */
public class EosProgressionSnapshot internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetProgressionSnapshotInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Begins a snapshot and returns the SDK-assigned snapshot id, or null if the call failed.
     *
     * `EOS_ProgressionSnapshot_BeginSnapshot` reports the new id through a `uint32_t*` out
     * parameter; the return value is only the [EosResult].
     */
    public fun beginSnapshot(
        localUserId: ProductUserId,
        snapshotId: String? = null,
    ): Int? = withCallArena { arena ->
        val options = ProgressionSnapshotBeginSnapshotOptions(localUserId, snapshotId)
        val outSnapshotId = arena.allocate(ValueLayout.JAVA_INT)
        val result = EosResult.fromValue(
            Native.invoke(
                "EOS_ProgressionSnapshot_BeginSnapshot",
                listOf(handle(), options.writeTo(arena), outSnapshotId),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (result == EosResult.Success) outSnapshotId.get(ValueLayout.JAVA_INT, 0) else null
    }

    public fun addProgression(
        snapshotId: String? = null,
        statName: String,
        statValue: Int,
    ): EosResult = withCallArena { arena ->
        val options = ProgressionSnapshotAddProgressionOptions(snapshotId, statName, statValue)
        EosResult.fromValue(
            Native.invoke(
                "EOS_ProgressionSnapshot_AddProgression",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun submitSnapshot(snapshotId: String? = null): CompletableFuture<EosResult> =
        asyncCall(
            "EOS_ProgressionSnapshot_SubmitSnapshot",
            ProgressionSnapshotSubmitSnapshotOptions(snapshotId),
        )

    /**
     * `SubmitSnapshot` and `DeleteSnapshot` are `void` C functions that deliver their result
     * through a completion delegate; neither returns an [EosResult] directly. (`BeginSnapshot`,
     * `AddProgression` and `EndSnapshot` really are synchronous.)
     */
    private fun asyncCall(function: String, options: StructWriter): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // Every EOS_ProgressionSnapshot_*CallbackInfo begins with EOS_EResult ResultCode at 0.
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

    public fun endSnapshot(snapshotId: String? = null): EosResult = withCallArena { arena ->
        val options = ProgressionSnapshotEndSnapshotOptions(snapshotId)
        EosResult.fromValue(
            Native.invoke(
                "EOS_ProgressionSnapshot_EndSnapshot",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    public fun deleteSnapshot(snapshotId: String, localUserId: ProductUserId): CompletableFuture<EosResult> =
        asyncCall(
            "EOS_ProgressionSnapshot_DeleteSnapshot",
            ProgressionSnapshotDeleteSnapshotOptions(localUserId, snapshotId),
        )
}