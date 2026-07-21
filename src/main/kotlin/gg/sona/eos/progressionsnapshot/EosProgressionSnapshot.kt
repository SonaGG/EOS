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
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

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

    public fun beginSnapshot(
        localUserId: ProductUserId,
        snapshotId: String? = null,
    ): EosResult = withCallArena { arena ->
        val options = ProgressionSnapshotBeginSnapshotOptions(localUserId, snapshotId)
        EosResult.fromValue(
            Native.invoke(
                "EOS_ProgressionSnapshot_BeginSnapshot",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
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

    public fun submitSnapshot(snapshotId: String? = null): EosResult = withCallArena { arena ->
        val options = ProgressionSnapshotSubmitSnapshotOptions(snapshotId)
        EosResult.fromValue(
            Native.invoke(
                "EOS_ProgressionSnapshot_SubmitSnapshot",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
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

    public fun deleteSnapshot(snapshotId: String, localUserId: ProductUserId): EosResult =
        withCallArena { arena ->
            val options = ProgressionSnapshotDeleteSnapshotOptions(localUserId, snapshotId)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_ProgressionSnapshot_DeleteSnapshot",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }
}

internal class ProgressionSnapshotBeginSnapshotOptions(
    var localUserId: ProductUserId,
    var snapshotId: String?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(snapshotId).address())
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class ProgressionSnapshotAddProgressionOptions(
    var snapshotId: String?,
    var statName: String,
    var statValue: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(snapshotId).address())
        seg.setInt64(16, arena.allocCString(statName).address())
        seg.setInt32(24, statValue)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class ProgressionSnapshotSubmitSnapshotOptions(var snapshotId: String?) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(snapshotId).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class ProgressionSnapshotEndSnapshotOptions(var snapshotId: String?) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(snapshotId).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class ProgressionSnapshotDeleteSnapshotOptions(
    var localUserId: ProductUserId,
    var snapshotId: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(snapshotId).address())
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
