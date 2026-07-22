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
package gg.sona.eos.lobby

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.*
import java.util.concurrent.CompletableFuture

/**
 * Lobby interface. Manages persistent multiplayer lobbies with built-in
 * support for voice chat, presence, and invites.
 */
public class EosLobby internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetLobbyInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    public fun createLobby(
        lobbyId: String,
        maxPlayers: Int,
        localUserId: EpicAccountId? = null,
        presenceEnabled: Boolean = true,
        allowInvites: Boolean = true,
        rtName: String? = null,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Lobby_CreateLobbyCallbackInfo: ResultCode@0, ClientData@8, LobbyId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyCreateLobbyOptions(
            localUserId, maxPlayers, presenceEnabled, allowInvites, lobbyId, rtName
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_CreateLobby",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun destroyLobby(lobbyId: String, localUserId: EpicAccountId? = null): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Lobby_DestroyLobbyCallbackInfo: ResultCode@0, ClientData@8, LobbyId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyDestroyLobbyOptions(lobbyId, localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_DestroyLobby",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun joinLobby(
        lobbyId: String,
        localUserId: EpicAccountId? = null,
        presenceEnabled: Boolean = true,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Lobby_JoinLobbyCallbackInfo: ResultCode@0, ClientData@8, LobbyId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyJoinLobbyOptions(lobbyId, localUserId, presenceEnabled)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_JoinLobby",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun leaveLobby(lobbyId: String, localUserId: EpicAccountId? = null): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Lobby_LeaveLobbyCallbackInfo: ResultCode@0, ClientData@8, LobbyId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyLeaveLobbyOptions(lobbyId, localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_LeaveLobby",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun updateLobby(
        lobbyId: String,
        localUserId: EpicAccountId? = null,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyUpdateLobbyOptions(lobbyId, localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_UpdateLobby",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun promoteMember(
        lobbyId: String,
        targetUserId: ProductUserId,
        localUserId: EpicAccountId? = null,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyPromoteMemberOptions(lobbyId, localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_PromoteMember",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun kickMember(
        lobbyId: String,
        targetUserId: ProductUserId,
        localUserId: EpicAccountId? = null,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyKickMemberOptions(lobbyId, localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_KickMember",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun hardMuteMember(
        lobbyId: String,
        targetUserId: ProductUserId,
        hardMute: Boolean,
        localUserId: EpicAccountId? = null,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyHardMuteMemberOptions(lobbyId, localUserId, targetUserId, hardMute)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_HardMuteMember",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getRTCRoomName(lobbyId: String): String? = withCallArena { arena ->
        val sizePtr = arena.allocate(ValueLayout.JAVA_INT)
        val ptr = arena.allocate(ValueLayout.ADDRESS)
        // First call: pass NULL to get the required size.
        val first = EosResult.fromValue(
            Native.invoke(
                "EOS_Lobby_GetRTCRoomName",
                listOf(handle(), arena.allocCString(lobbyId).address(), 0L, sizePtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        if (first != EosResult.Success && first != EosResult.Success) return@withCallArena null
        val size = sizePtr.get(ValueLayout.JAVA_INT, 0)
        if (size <= 0) return@withCallArena null
        // Second call: get the actual string.
        val buf = arena.allocate(size.toLong())
        EosResult.fromValue(
            Native.invoke(
                "EOS_Lobby_GetRTCRoomName",
                listOf(handle(), arena.allocCString(lobbyId).address(), buf.address(), sizePtr),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
        buf.reinterpret(Long.MAX_VALUE).getString(0)
    }

    public fun sendInvite(
        lobbyId: String,
        targetUserId: EpicAccountId,
        localUserId: EpicAccountId? = null,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbySendInviteOptions(lobbyId, localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_SendInvite",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun rejectInvite(lobbyId: String, localUserId: EpicAccountId? = null): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyRejectInviteOptions(lobbyId, localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_RejectInvite",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun queryInvites(localUserId: EpicAccountId? = null): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = LobbyQueryInvitesOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Lobby_QueryInvites",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getInviteCount(localUserId: EpicAccountId? = null): Int {
        val options = LobbyGetInviteCountOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Lobby_GetInviteCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    public fun addNotifyLobbyUpdateReceived(
        callback: (LobbyUpdateReceivedInfo) -> Unit,
    ): NotificationHandle = addNotify(
        "EOS_Lobby_AddNotifyLobbyUpdateReceived",
        "EOS_Lobby_RemoveNotifyLobbyUpdateReceived",
    ) { data ->
        val lobbyId = readString(data, 16)
        callback(LobbyUpdateReceivedInfo(lobbyId))
    }

    public fun addNotifyLobbyMemberUpdateReceived(
        callback: (LobbyMemberUpdateReceivedInfo) -> Unit,
    ): NotificationHandle = addNotify(
        "EOS_Lobby_AddNotifyLobbyMemberUpdateReceived",
        "EOS_Lobby_RemoveNotifyLobbyMemberUpdateReceived",
    ) { data ->
        val lobbyId = readString(data, 16)
        callback(LobbyMemberUpdateReceivedInfo(lobbyId))
    }

    public fun addNotifyLobbyMemberStatusReceived(
        callback: (LobbyMemberStatusReceivedInfo) -> Unit,
    ): NotificationHandle = addNotify(
        "EOS_Lobby_AddNotifyLobbyMemberStatusReceived",
        "EOS_Lobby_RemoveNotifyLobbyMemberStatusReceived",
    ) { data ->
        val lobbyId = readString(data, 16)
        val targetUserId = ProductUserId(data.getInt64(24))
        val currentStatus = EosLobbyMemberStatus.fromValue(data.getInt32(32))
        val previousStatus = EosLobbyMemberStatus.fromValue(data.getInt32(36))
        callback(LobbyMemberStatusReceivedInfo(lobbyId, targetUserId, currentStatus, previousStatus))
    }

    public fun removeNotifyLobbyUpdateReceived(handle: NotificationHandle) =
        unregisterNotify("EOS_Lobby_RemoveNotifyLobbyUpdateReceived", handle)

    public fun removeNotifyLobbyMemberUpdateReceived(handle: NotificationHandle) =
        unregisterNotify("EOS_Lobby_RemoveNotifyLobbyMemberUpdateReceived", handle)

    public fun removeNotifyLobbyMemberStatusReceived(handle: NotificationHandle) =
        unregisterNotify("EOS_Lobby_RemoveNotifyLobbyMemberStatusReceived", handle)

    private fun addNotify(
        addFn: String,
        removeFn: String,
        parse: (MemorySegment) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data -> parse(data) }
        val handle = CallbackStubs.register(invoker)
        val options = LobbyAddNotifyCommonOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                addFn,
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    private fun unregisterNotify(removeFn: String, handle: NotificationHandle) {
        Native.invokeVoid(
            removeFn,
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

