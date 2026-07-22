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
package gg.sona.eos.presence

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.*
import java.lang.foreign.*
import java.util.concurrent.CompletableFuture

/** Presence interface for online status and rich presence. */
public class EosPresence internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetPresenceInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    public fun queryPresence(localUserId: EpicAccountId, targetUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Presence_QueryPresenceCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = PresenceQueryPresenceOptions(localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Presence_QueryPresence",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun hasPresence(localUserId: EpicAccountId, targetUserId: EpicAccountId): Boolean =
        withCallArena { arena ->
            val options = PresenceHasPresenceOptions(localUserId, targetUserId)
            Native.invoke(
                "EOS_Presence_HasPresence",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int != 0
        }

    public fun addNotifyOnPresenceChanged(
        localUserId: EpicAccountId,
        callback: (PresenceChangedInfo) -> Unit,
    ): NotificationHandle {
        // EOS_Presence_PresenceChangedCallbackInfo: ClientData@0, LocalUserId@8, PresenceUserId@16
        val invoker = EosCallback { data ->
            val localUserId = EpicAccountId(data.getInt64(8))
            val presenceUserId = EpicAccountId(data.getInt64(16))
            callback(PresenceChangedInfo(localUserId, presenceUserId))
        }
        val handle = CallbackStubs.register(invoker)
        val options = PresenceAddNotifyOnPresenceChangedOptions(localUserId)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Presence_AddNotifyOnPresenceChanged",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyOnPresenceChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_Presence_RemoveNotifyOnPresenceChanged",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }
}
