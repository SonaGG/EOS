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
package gg.sona.eos.sessions

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.allocCStringArray
import gg.sona.eos.internal.allocHandleArray
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
 * Sessions interface. Manages match-making sessions, including creation,
 * joining, player registration, invites, and search.
 */
public class EosSessions internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetSessionsInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    // region Session lifecycle

    public fun createSession(
        sessionName: String,
        bucketId: String,
        maxPlayers: Int,
        localUserId: EpicAccountId? = null,
        presenceEnabled: Boolean = true,
    ): CompletableFuture<CreateOrUpdateSessionResult> {
        val future = CompletableFuture<CreateOrUpdateSessionResult>()
        // EOS_Sessions_UpdateSessionCallbackInfo: ResultCode@0, ClientData@8, SessionName@16, SessionId@24
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val sessionId = readString(data, 24)
            val sessionNameOut = readString(data, 16)
            future.complete(CreateOrUpdateSessionResult(result, sessionId, sessionNameOut))
        })
        CallbackStubs.release(stub.id)
        throw UnsupportedOperationException(
            "The EOS SDK has no EOS_Sessions_CreateSession function. Creating a session is a " +
                "two-step flow: EOS_Sessions_CreateSessionModification to obtain an " +
                "EOS_HSessionModification, configure it, then EOS_Sessions_UpdateSession to " +
                "commit it. That modification-handle API is not yet bound in this library."
        )
    }

    public fun updateSession(
        sessionName: String,
    ): CompletableFuture<CreateOrUpdateSessionResult> {
        val future = CompletableFuture<CreateOrUpdateSessionResult>()
        // EOS_Sessions_UpdateSessionCallbackInfo: ResultCode@0, ClientData@8, SessionName@16, SessionId@24
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val sessionId = readString(data, 24)
            val sessionNameOut = readString(data, 16)
            future.complete(CreateOrUpdateSessionResult(result, sessionId, sessionNameOut))
        })
        val options = SessionsUpdateSessionOptions(sessionName)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_UpdateSession",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun destroySession(sessionName: String): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Sessions_DestroySessionCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = SessionsDestroySessionOptions(sessionName)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_DestroySession",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun joinSession(
        sessionName: String,
        sessionHandle: SessionHandle,
        localUserId: EpicAccountId? = null,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Sessions_JoinSessionCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = SessionsJoinSessionOptions(sessionName, sessionHandle, localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_JoinSession",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun startSession(sessionName: String): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Sessions_StartSessionCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = SessionsStartSessionOptions(sessionName)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_StartSession",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun endSession(sessionName: String): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Sessions_EndSessionCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = SessionsEndSessionOptions(sessionName)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_EndSession",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun registerPlayers(
        sessionName: String,
        players: List<ProductUserId>,
    ): CompletableFuture<RegisterPlayersResult> {
        val future = CompletableFuture<RegisterPlayersResult>()
        // EOS_Sessions_RegisterPlayersCallbackInfo: ResultCode@0, ClientData@8, RegisteredPlayers@16, RegisteredPlayersCount@24
        // NOTE: this struct has no SessionName field at all (only Register/SanctionedPlayers arrays), so
        // sessionNameOut cannot be fixed by an offset change alone - left unchanged.
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val sessionNameOut = readString(data, 16)
            val registeredCount = data.getInt32(24).toLong() and 0xffffffffL
            future.complete(RegisterPlayersResult(result, sessionNameOut, registeredCount.toInt()))
        })
        val options = SessionsRegisterPlayersOptions(sessionName, players)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_RegisterPlayers",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun unregisterPlayers(
        sessionName: String,
        players: List<ProductUserId>,
    ): CompletableFuture<RegisterPlayersResult> {
        val future = CompletableFuture<RegisterPlayersResult>()
        // EOS_Sessions_UnregisterPlayersCallbackInfo: ResultCode@0, ClientData@8, UnregisteredPlayers@16, UnregisteredPlayersCount@24
        // NOTE: this struct has no SessionName field at all, so sessionNameOut cannot be fixed by an
        // offset change alone - left unchanged.
        val stub = CallbackStubs.register(EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(0))
            val sessionNameOut = readString(data, 16)
            val unregisteredCount = data.getInt32(24).toLong() and 0xffffffffL
            future.complete(RegisterPlayersResult(result, sessionNameOut, unregisteredCount.toInt()))
        })
        val options = SessionsUnregisterPlayersOptions(sessionName, players)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_UnregisterPlayers",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    // endregion

    // region Invites

    public fun sendInvite(
        sessionName: String,
        localUserId: EpicAccountId,
        targetUserId: EpicAccountId,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Sessions_SendInviteCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = SessionsSendInviteOptions(sessionName, localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_SendInvite",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun rejectInvite(
        sessionName: String,
        localUserId: EpicAccountId,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Sessions_RejectInviteCallbackInfo: ResultCode@0, ClientData@8
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = SessionsRejectInviteOptions(sessionName, localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_RejectInvite",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun queryInvites(localUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_Sessions_QueryInvitesCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = SessionsQueryInvitesOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Sessions_QueryInvites",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun getInviteCount(localUserId: EpicAccountId): Int {
        val options = SessionsGetInviteCountOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Sessions_GetInviteCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    /**
     * `EOS_Sessions_GetInviteIdByIndex(Handle, Options, char* OutBuffer, int32_t* InOutBufferLength)`
     * writes into a caller-supplied buffer; it does not hand back a pointer. Called once with a
     * null buffer to learn the required length, then again to fill it.
     */
    public fun getInviteIdByIndex(localUserId: EpicAccountId, index: Int): String? =
        withCallArena { arena ->
            val options = SessionsGetInviteIdByIndexOptions(localUserId, index)
            val lengthPtr = arena.allocate(ValueLayout.JAVA_INT)
            val sized = EosResult.fromValue(
                Native.invoke(
                    "EOS_Sessions_GetInviteIdByIndex",
                    listOf(handle(), options.writeTo(arena), MemorySegment.NULL, lengthPtr),
                    listOf(
                        ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (sized != EosResult.Success && sized != EosResult.LimitExceeded) {
                return@withCallArena null
            }
            val length = lengthPtr.get(ValueLayout.JAVA_INT, 0)
            if (length <= 0) return@withCallArena null

            val outBuffer = arena.allocate(length.toLong())
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_Sessions_GetInviteIdByIndex",
                    listOf(handle(), options.writeTo(arena), outBuffer, lengthPtr),
                    listOf(
                        ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                    ),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            return@withCallArena outBuffer.getString(0)
        }

    // endregion

    public fun addNotifySessionInviteReceived(
        callback: (SessionInviteReceivedInfo) -> Unit,
    ): NotificationHandle = addNotify(
        "EOS_Sessions_AddNotifySessionInviteReceived",
        "EOS_Sessions_RemoveNotifySessionInviteReceived",
    ) { data ->
        // EOS_Sessions_SessionInviteReceivedCallbackInfo: ClientData@0, LocalUserId@8, TargetUserId@16, InviteId@24
        // (struct has no distinct SessionId field; InviteId is read here in its place)
        val sessionId = readString(data, 24)
        val fromUserId = EpicAccountId(data.getInt64(16))
        callback(SessionInviteReceivedInfo(sessionId, fromUserId))
    }

    public fun addNotifySessionInviteAccepted(
        callback: (SessionInviteAcceptedInfo) -> Unit,
    ): NotificationHandle = addNotify(
        "EOS_Sessions_AddNotifySessionInviteAccepted",
        "EOS_Sessions_RemoveNotifySessionInviteAccepted",
    ) { data ->
        // EOS_Sessions_SessionInviteAcceptedCallbackInfo: ClientData@0, SessionId@8, LocalUserId@16, TargetUserId@24, InviteId@32
        val sessionId = readString(data, 8)
        val fromUserId = EpicAccountId(data.getInt64(24))
        val localUserId = EpicAccountId(data.getInt64(16))
        callback(SessionInviteAcceptedInfo(sessionId, fromUserId, localUserId))
    }

    public fun removeNotifySessionInviteReceived(handle: NotificationHandle) =
        unregisterNotify("EOS_Sessions_RemoveNotifySessionInviteReceived", handle)

    public fun removeNotifySessionInviteAccepted(handle: NotificationHandle) =
        unregisterNotify("EOS_Sessions_RemoveNotifySessionInviteAccepted", handle)

    private fun addNotify(
        addFn: String,
        removeFn: String,
        parse: (MemorySegment) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data -> parse(data) }
        val handle = CallbackStubs.register(invoker)
        val options = SessionsAddNotifyCommonOptions()
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

public class CreateOrUpdateSessionResult(
    public val result: EosResult,
    public val sessionId: String,
    public val sessionName: String,
)

public class RegisterPlayersResult(
    public val result: EosResult,
    public val sessionName: String,
    public val affectedCount: Int,
)

public class SessionInviteReceivedInfo(
    public val sessionId: String,
    public val fromUserId: EpicAccountId,
)

public class SessionInviteAcceptedInfo(
    public val sessionId: String,
    public val fromUserId: EpicAccountId,
    public val localUserId: EpicAccountId,
)

/** Opaque handle to an active session, used for join operations. */
@JvmInline
public value class SessionHandle(public val raw: Long) {
    public fun isValid(): Boolean = raw != 0L

    public companion object {
        public val Invalid: SessionHandle = SessionHandle(0L)
    }
}

// region Struct writers

internal class SessionsCreateSessionOptions(
    var sessionName: String,
    var bucketId: String,
    var maxPlayers: Int,
    var localUserId: EpicAccountId?,
    var presenceEnabled: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        seg.setInt64(16, arena.allocCString(bucketId).address())
        seg.setInt32(24, maxPlayers)
        seg.setInt64(32, localUserId?.raw ?: 0L)
        seg.setInt32(40, if (presenceEnabled) 1 else 0)
        return seg
    }

    companion object {
        const val API_LATEST = 5
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class SessionsUpdateSessionOptions(var sessionName: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class SessionsDestroySessionOptions(var sessionName: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class SessionsJoinSessionOptions(
    var sessionName: String,
    var sessionHandle: SessionHandle,
    var localUserId: EpicAccountId?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        seg.setInt64(16, sessionHandle.raw)
        seg.setInt64(24, localUserId?.raw ?: 0L)
        return seg
    }

    companion object {
        const val API_LATEST = 2
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class SessionsStartSessionOptions(var sessionName: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class SessionsEndSessionOptions(var sessionName: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS
        )
    }
}

internal class SessionsRegisterPlayersOptions(
    var sessionName: String,
    var players: List<ProductUserId>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        val arr = arena.allocHandleArray(players.map { it.raw })
        seg.setInt64(16, arr.address())
        seg.setInt32(24, players.size)
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

internal class SessionsUnregisterPlayersOptions(
    var sessionName: String,
    var players: List<ProductUserId>,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        val arr = arena.allocHandleArray(players.map { it.raw })
        seg.setInt64(16, arr.address())
        seg.setInt32(24, players.size)
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

internal class SessionsSendInviteOptions(
    var sessionName: String,
    var localUserId: EpicAccountId,
    var targetUserId: EpicAccountId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        seg.setInt64(16, localUserId.raw)
        seg.setInt64(24, targetUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
        )
    }
}

internal class SessionsRejectInviteOptions(
    var sessionName: String,
    var localUserId: EpicAccountId,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, arena.allocCString(sessionName).address())
        seg.setInt64(16, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
        )
    }
}

internal class SessionsQueryInvitesOptions(var localUserId: EpicAccountId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}

internal class SessionsGetInviteCountOptions(var localUserId: EpicAccountId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}

internal class SessionsGetInviteIdByIndexOptions(
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
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
        )
    }
}

internal class SessionsAddNotifyCommonOptions : StructWriter {
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

// endregion
