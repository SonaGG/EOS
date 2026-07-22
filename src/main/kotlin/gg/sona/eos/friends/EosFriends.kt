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
package gg.sona.eos.friends

import gg.sona.eos.NotificationHandle
import gg.sona.eos.internal.setInt8
import gg.sona.eos.internal.setInt16
import gg.sona.eos.internal.setFloat
import gg.sona.eos.internal.setDouble
import gg.sona.eos.internal.setBool
import gg.sona.eos.internal.getInt8
import gg.sona.eos.internal.getInt16
import gg.sona.eos.internal.getInt32
import gg.sona.eos.internal.getInt64
import gg.sona.eos.internal.getFloat
import gg.sona.eos.internal.getDouble
import gg.sona.eos.internal.getBool

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
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
import java.util.concurrent.CompletableFuture

/** Friends interface for managing friend lists and invites. */
public class EosFriends internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetFriendsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    public fun queryFriends(localUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            // EOS_Friends_QueryFriendsCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = FriendsQueryFriendsOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Friends_QueryFriends",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun sendInvite(localUserId: EpicAccountId, targetUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            // EOS_Friends_SendInviteCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = FriendsSendInviteOptions(localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Friends_SendInvite",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun acceptInvite(localUserId: EpicAccountId, targetUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            // EOS_Friends_AcceptInviteCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = FriendsAcceptInviteOptions(localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Friends_AcceptInvite",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun rejectInvite(localUserId: EpicAccountId, targetUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            // EOS_Friends_RejectInviteCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = FriendsRejectInviteOptions(localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Friends_RejectInvite",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getFriendsCount(localUserId: EpicAccountId): Int {
        val fn = Native.downcall(
            "EOS_Friends_GetFriendsCount",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(handle(), localUserId.raw) as Int
    }

    public fun getFriendAtIndex(localUserId: EpicAccountId, index: Int): EpicAccountId =
        withCallArena { arena ->
            val options = FriendsGetFriendAtIndexOptions(localUserId, index)
            EpicAccountId(
                Native.invoke(
                    "EOS_Friends_GetFriendAtIndex",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_LONG,
                ) as Long
            )
        }

    public fun getStatus(localUserId: EpicAccountId, targetUserId: EpicAccountId): EosFriendsStatus =
        withCallArena { arena ->
            val options = FriendsGetStatusOptions(localUserId, targetUserId)
            EosFriendsStatus.fromValue(
                Native.invoke(
                    "EOS_Friends_GetStatus",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }

    public fun addNotifyFriendsUpdate(
        localUserId: EpicAccountId,
        callback: (FriendsUpdateInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            // EOS_Friends_OnFriendsUpdateInfo: ClientData@0, LocalUserId@8, TargetUserId@16, PreviousStatus@24, CurrentStatus@28
            val localUserId = EpicAccountId(data.getInt64(8))
            val targetUserId = EpicAccountId(data.getInt64(16))
            val previous = EosFriendsStatus.fromValue(data.getInt32(24))
            val current = EosFriendsStatus.fromValue(data.getInt32(28))
            callback(FriendsUpdateInfo(localUserId, targetUserId, previous, current))
        }
        val handle = CallbackStubs.register(invoker)
        val options = FriendsAddNotifyFriendsUpdateOptions(localUserId)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Friends_AddNotifyFriendsUpdate",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyFriendsUpdate(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_Friends_RemoveNotifyFriendsUpdate",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun getBlockedUsersCount(localUserId: EpicAccountId): Int {
        val fn = Native.downcall(
            "EOS_Friends_GetBlockedUsersCount",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(handle(), localUserId.raw) as Int
    }

    public fun getBlockedUserAtIndex(localUserId: EpicAccountId, index: Int): EpicAccountId =
        withCallArena { arena ->
            val options = FriendsGetBlockedUserAtIndexOptions(localUserId, index)
            EpicAccountId(
                Native.invoke(
                    "EOS_Friends_GetBlockedUserAtIndex",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_LONG,
                ) as Long
            )
        }
}

/** `EOS_Friends_GetFriendAtIndexOptions`: ApiVersion@0, LocalUserId@8, Index@16. */
internal class FriendsGetFriendAtIndexOptions(
    var localUserId: EpicAccountId,
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
        // EOS_FRIENDS_GETFRIENDATINDEX_API_LATEST
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

/** `EOS_Friends_GetStatusOptions`: ApiVersion@0, LocalUserId@8, TargetUserId@16. */
internal class FriendsGetStatusOptions(
    var localUserId: EpicAccountId,
    var targetUserId: EpicAccountId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, targetUserId.raw)
        return seg
    }

    companion object {
        // EOS_FRIENDS_GETSTATUS_API_LATEST
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

/** `EOS_Friends_GetBlockedUserAtIndexOptions`: ApiVersion@0, LocalUserId@8, Index@16. */
internal class FriendsGetBlockedUserAtIndexOptions(
    var localUserId: EpicAccountId,
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
        // EOS_FRIENDS_GETBLOCKEDUSERATINDEX_API_LATEST
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

/** Status between two friends. */
public enum class EosFriendsStatus(val value: Int) {
    NotFriends(0),
    InviteSent(1),
    InviteReceived(2),
    Friends(3);

    public companion object {
        public fun fromValue(v: Int): EosFriendsStatus = entries.firstOrNull { it.value == v } ?: NotFriends
    }
}

public class FriendsUpdateInfo(
    public val localUserId: EpicAccountId,
    public val targetUserId: EpicAccountId,
    public val previousStatus: EosFriendsStatus,
    public val currentStatus: EosFriendsStatus,
)

internal class FriendsQueryFriendsOptions(var localUserId: EpicAccountId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class FriendsSendInviteOptions(
    var localUserId: EpicAccountId,
    var targetUserId: EpicAccountId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, targetUserId.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class FriendsAcceptInviteOptions(
    var localUserId: EpicAccountId,
    var targetUserId: EpicAccountId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, targetUserId.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class FriendsRejectInviteOptions(
    var localUserId: EpicAccountId,
    var targetUserId: EpicAccountId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, targetUserId.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class FriendsAddNotifyFriendsUpdateOptions(var localUserId: EpicAccountId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}
