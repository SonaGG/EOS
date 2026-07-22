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
package gg.sona.eos.reports

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
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.getInt32
import java.util.concurrent.CompletableFuture

/**
 * Reports interface. Submit player-behavior reports.
 */
public class EosReports internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetReportsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Submit a behavior report against another user.
     *
     * @param reporterUserId the user submitting the report
     * @param reportedUserId the user being reported
     * @param reasonCategory the category of behavior (game-defined; e.g. "Griefing")
     * @param comments free-form text describing the behavior
     * @param contextBlob arbitrary additional context (may be empty)
     */
    public fun sendPlayerBehaviorReport(
        reporterUserId: ProductUserId,
        reportedUserId: ProductUserId,
        reasonCategory: String,
        comments: String = "",
        contextBlob: ByteArray = ByteArray(0),
    ): CompletableFuture<EosResult> = withCallArena { arena ->
        val future = CompletableFuture<EosResult>()
        val ctx = arena.allocate(contextBlob.size.toLong())
        if (contextBlob.isNotEmpty()) {
            ctx.copyFrom(MemorySegment.ofArray(contextBlob))
        }
        val options = ReportsSendPlayerBehaviorReportOptions(
            reporterUserId, reportedUserId, reasonCategory, comments, contextBlob.size, ctx
        )
        // EOS_Reports_SendPlayerBehaviorReportCompleteCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        Native.invokeVoid(
            "EOS_Reports_SendPlayerBehaviorReport",
            listOf(handle(), options.writeTo(arena), MemorySegment.NULL, stub.segment),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        )
        future
    }
}

internal class ReportsSendPlayerBehaviorReportOptions(
    var reporterUserId: ProductUserId,
    var reportedUserId: ProductUserId,
    var reasonCategory: String,
    var comments: String,
    var contextLength: Int,
    var context: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, reporterUserId.raw)
        seg.setInt64(16, reportedUserId.raw)
        seg.setInt64(24, arena.allocCString(reasonCategory).address())
        seg.setInt64(32, arena.allocCString(comments).address())
        seg.setInt32(40, contextLength)
        seg.setInt64(48, context.address())
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}
