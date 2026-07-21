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
package gg.sona.eos.custominvites

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Custom Invites interface. Game-defined invite payloads.
 */
public class EosCustomInvites internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetCustomInvitesInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /** Set the custom invite payload for the local user. */
    public fun setCustomInvite(
        localUserId: ProductUserId,
        payload: String,
    ): EosResult = withCallArena { arena ->
        val options = CustomInvitesSetCustomInviteOptions(localUserId, payload)
        EosResult.fromValue(
            Native.invoke(
                "EOS_CustomInvites_SetCustomInvite",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    /** Send the previously-set custom invite to a list of recipients. */
    public fun sendCustomInvite(
        localUserId: ProductUserId,
        recipients: List<ProductUserId>,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = CustomInvitesSendCustomInviteOptions(localUserId, recipients)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_CustomInvites_SendCustomInvite",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun addNotifyCustomInviteReceived(
        callback: (CustomInviteReceivedInfo) -> Unit,
    ): NotificationHandle = registerCustomInviteListener(
        "EOS_CustomInvites_AddNotifyCustomInviteReceived",
        "EOS_CustomInvites_RemoveNotifyCustomInviteReceived",
    ) { data ->
        val inviteId = readString(data, 16)
        val fromUserId = ProductUserId(data.getInt64(24))
        val payload = readString(data, 32)
        callback(CustomInviteReceivedInfo(inviteId, fromUserId, payload))
    }

    public fun addNotifyCustomInviteAccepted(
        callback: (CustomInviteAcceptedInfo) -> Unit,
    ): NotificationHandle = registerCustomInviteListener(
        "EOS_CustomInvites_AddNotifyCustomInviteAccepted",
        "EOS_CustomInvites_RemoveNotifyCustomInviteAccepted",
    ) { data ->
        val inviteId = readString(data, 16)
        val fromUserId = ProductUserId(data.getInt64(24))
        val payload = readString(data, 32)
        callback(CustomInviteAcceptedInfo(inviteId, fromUserId, payload))
    }

    public fun addNotifyCustomInviteRejected(
        callback: (CustomInviteRejectedInfo) -> Unit,
    ): NotificationHandle = registerCustomInviteListener(
        "EOS_CustomInvites_AddNotifyCustomInviteRejected",
        "EOS_CustomInvites_RemoveNotifyCustomInviteRejected",
    ) { data ->
        val inviteId = readString(data, 16)
        val fromUserId = ProductUserId(data.getInt64(24))
        val payload = readString(data, 32)
        callback(CustomInviteRejectedInfo(inviteId, fromUserId, payload))
    }

    public fun removeNotifyCustomInviteReceived(handle: NotificationHandle) =
        unregisterCustomInviteListener(
            "EOS_CustomInvites_RemoveNotifyCustomInviteReceived", handle
        )

    public fun removeNotifyCustomInviteAccepted(handle: NotificationHandle) =
        unregisterCustomInviteListener(
            "EOS_CustomInvites_RemoveNotifyCustomInviteAccepted", handle
        )

    public fun removeNotifyCustomInviteRejected(handle: NotificationHandle) =
        unregisterCustomInviteListener(
            "EOS_CustomInvites_RemoveNotifyCustomInviteRejected", handle
        )

    /** Finalize a previously-received custom invite (mark as handled). */
    public fun finalizeInvite(
        localUserId: ProductUserId,
        inviteId: String,
        processingResult: EosCustomInviteProcessingResult,
    ): EosResult = withCallArena { arena ->
        val options = CustomInvitesFinalizeInviteOptions(
            localUserId, inviteId, processingResult
        )
        EosResult.fromValue(
            Native.invoke(
                "EOS_CustomInvites_FinalizeInvite",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    private fun registerCustomInviteListener(
        addFunction: String,
        @Suppress("UNUSED_PARAMETER") removeFunction: String,
        parse: (MemorySegment) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data -> parse(data) }
        val handle = CallbackStubs.register(invoker)
        val options = CustomInvitesAddNotifyCommonOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                addFunction,
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    private fun unregisterCustomInviteListener(
        removeFunction: String,
        handle: NotificationHandle,
    ) {
        Native.invokeVoid(
            removeFunction,
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    private fun readString(seg: MemorySegment, offset: Long): String {
        val ptr = seg.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return ""
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }
}

/** Result of a custom invite. */
public enum class EosCustomInviteProcessingResult(val value: Int) {
    Unknown(0),
    Accepted(1),
    Deferred(2),
    UnhandledError(3);

    public companion object {
        internal fun fromValue(v: Int): EosCustomInviteProcessingResult =
            entries.firstOrNull { it.value == v } ?: Unknown
    }
}

public class CustomInviteReceivedInfo(
    public val inviteId: String,
    public val fromUserId: ProductUserId,
    public val payload: String,
)

public class CustomInviteAcceptedInfo(
    public val inviteId: String,
    public val fromUserId: ProductUserId,
    public val payload: String,
)

public class CustomInviteRejectedInfo(
    public val inviteId: String,
    public val fromUserId: ProductUserId,
    public val payload: String,
)

// region Struct writers

internal class CustomInvitesSetCustomInviteOptions(
    var localUserId: ProductUserId,
    var payload: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(payload).address())
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

internal class CustomInvitesSendCustomInviteOptions(
    var localUserId: ProductUserId,
    var recipients: List<ProductUserId>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        val arr = arena.allocate(ValueLayout.JAVA_LONG, recipients.size.toLong())
        recipients.forEachIndexed { i, id ->
            arr.setAtIndex(ValueLayout.JAVA_LONG, i.toLong(), id.raw)
        }
        seg.setInt64(16, arr.address())
        seg.setInt32(24, recipients.size)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class CustomInvitesAddNotifyCommonOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4)
        )
    }
}

internal class CustomInvitesFinalizeInviteOptions(
    var localUserId: ProductUserId,
    var inviteId: String,
    var processingResult: EosCustomInviteProcessingResult,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(inviteId).address())
        seg.setInt32(24, processingResult.value)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}
