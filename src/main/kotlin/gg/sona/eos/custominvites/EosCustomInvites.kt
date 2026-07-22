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
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Custom Invites interface. Game-defined invite payloads.
 */
class EosCustomInvites internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetCustomInvitesInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /** Set the custom invite payload for the local user. */
    fun setCustomInvite(
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
    fun sendCustomInvite(
        localUserId: ProductUserId,
        recipients: List<ProductUserId>,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_CustomInvites_SendCustomInviteCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserIds@24, TargetUserIdsCount@32
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = CustomInvitesSendCustomInviteOptions(localUserId, recipients)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_CustomInvites_SendCustomInvite",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun addNotifyCustomInviteReceived(
        callback: (CustomInviteReceivedInfo) -> Unit,
    ): NotificationHandle = registerCustomInviteListener(
        "EOS_CustomInvites_AddNotifyCustomInviteReceived",
        "EOS_CustomInvites_RemoveNotifyCustomInviteReceived",
    ) { data ->
        // EOS_CustomInvites_OnCustomInviteReceivedCallbackInfo: ClientData@0, TargetUserId@8, LocalUserId@16, CustomInviteId@24, Payload@32
        val inviteId = readString(data, 24)
        val fromUserId = ProductUserId(data.getInt64(8))
        val payload = readString(data, 32)
        callback(CustomInviteReceivedInfo(inviteId, fromUserId, payload))
    }

    fun addNotifyCustomInviteAccepted(
        callback: (CustomInviteAcceptedInfo) -> Unit,
    ): NotificationHandle = registerCustomInviteListener(
        "EOS_CustomInvites_AddNotifyCustomInviteAccepted",
        "EOS_CustomInvites_RemoveNotifyCustomInviteAccepted",
    ) { data ->
        // EOS_CustomInvites_OnCustomInviteAcceptedCallbackInfo: ClientData@0, TargetUserId@8, LocalUserId@16, CustomInviteId@24, Payload@32
        val inviteId = readString(data, 24)
        val fromUserId = ProductUserId(data.getInt64(8))
        val payload = readString(data, 32)
        callback(CustomInviteAcceptedInfo(inviteId, fromUserId, payload))
    }

    fun addNotifyCustomInviteRejected(
        callback: (CustomInviteRejectedInfo) -> Unit,
    ): NotificationHandle = registerCustomInviteListener(
        "EOS_CustomInvites_AddNotifyCustomInviteRejected",
        "EOS_CustomInvites_RemoveNotifyCustomInviteRejected",
    ) { data ->
        // EOS_CustomInvites_CustomInviteRejectedCallbackInfo: ClientData@0, TargetUserId@8, LocalUserId@16, CustomInviteId@24, Payload@32
        val inviteId = readString(data, 24)
        val fromUserId = ProductUserId(data.getInt64(8))
        val payload = readString(data, 32)
        callback(CustomInviteRejectedInfo(inviteId, fromUserId, payload))
    }

    fun removeNotifyCustomInviteReceived(handle: NotificationHandle) =
        unregisterCustomInviteListener(
            "EOS_CustomInvites_RemoveNotifyCustomInviteReceived", handle
        )

    fun removeNotifyCustomInviteAccepted(handle: NotificationHandle) =
        unregisterCustomInviteListener(
            "EOS_CustomInvites_RemoveNotifyCustomInviteAccepted", handle
        )

    fun removeNotifyCustomInviteRejected(handle: NotificationHandle) =
        unregisterCustomInviteListener(
            "EOS_CustomInvites_RemoveNotifyCustomInviteRejected", handle
        )

    /** Finalize a previously-received custom invite (mark as handled). */
    fun finalizeInvite(
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
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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