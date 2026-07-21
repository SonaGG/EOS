/*
 * Copyright 2024 sona
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
package gg.sona.eos.auth

import gg.sona.eos.NotificationHandle
import gg.sona.eos.internal.setInt8
import gg.sona.eos.internal.setInt16
import gg.sona.eos.internal.setInt32
import gg.sona.eos.internal.setInt64
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
import gg.sona.eos.common.EosLoginStatus
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Auth interface for Epic Account login and token management.
 */
public class EosAuth internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetAuthInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /**
     * Log in with the supplied credentials. The credential type is one of
     * [EosAuthCredentialType] (e.g. [EosAuthCredentialType.Developer], [EosAuthCredentialType.RefreshToken],
     * [EosAuthCredentialType.ExternalAuth]).
     */
    public fun login(
        credentialType: EosAuthCredentialType,
        credentialId: String? = null,
        credentialToken: String? = null,
        continueLoginToken: EpicAccountId? = null,
        scopeFlags: Set<EosAuthScopeFlags> = emptySet(),
    ): CompletableFuture<LoginResult> {
        val future = CompletableFuture<LoginResult>()
        val invoker = EosCallback { data ->
            val result = EosResult.fromValue(data.getInt32(8))
            val localUserId = EpicAccountId(data.getInt64(24))
            future.complete(LoginResult(result, localUserId))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AuthLoginOptions(
            credentialType = credentialType,
            id = credentialId,
            token = credentialToken,
            continueLoginToken = continueLoginToken,
            scopeFlags = scopeFlags.fold(0) { acc, f -> acc or f.value },
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Auth_Login",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun logout(localUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val invoker = EosCallback { data -> future.complete(EosResult.fromValue(data.getInt32(8))) }
        val handle = CallbackStubs.register(invoker)
        val options = AuthLogoutOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_Auth_Logout",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /** Get the number of locally logged-in accounts. */
    public fun getLoggedInAccountsCount(): Int {
        val fn = Native.downcall(
            "EOS_Auth_GetLoggedInAccountsCount",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(handle()) as Int
    }

    /** Get a logged-in account by index. */
    public fun getLoggedInAccountByIndex(index: Int): EpicAccountId {
        val fn = Native.downcall(
            "EOS_Auth_GetLoggedInAccountByIndex",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)
        )
        return EpicAccountId(fn.invokeExact(handle(), index) as Long)
    }

    public fun getLoginStatus(localUserId: EpicAccountId): EosLoginStatus {
        val fn = Native.downcall(
            "EOS_Auth_GetLoginStatus",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return EosLoginStatus.fromValue(fn.invokeExact(handle(), localUserId.raw) as Int)
    }

    public fun addNotifyLoginStatusChanged(
        callback: (LoginStatusChangedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = EpicAccountId(data.getInt64(16))
            val prevStatus = EosLoginStatus.fromValue(data.getInt32(24))
            val newStatus = EosLoginStatus.fromValue(data.getInt32(28))
            callback(LoginStatusChangedInfo(localUserId, prevStatus, newStatus))
        }
        val handle = CallbackStubs.register(invoker)
        val options = AuthAddNotifyLoginStatusChangedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_Auth_AddNotifyLoginStatusChanged",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyLoginStatusChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_Auth_RemoveNotifyLoginStatusChanged",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }
}

/** Type of credentials used to log in. */
public enum class EosAuthCredentialType(val value: Int) {
    Developer(0),
    NoUser(1),
    UserLogin(2),
    UserPassword(3),
    RefreshToken(4),
    ExternalAuth(5);

    public companion object {
        public fun fromValue(v: Int): EosAuthCredentialType = entries.firstOrNull { it.value == v } ?: NoUser
    }
}

/** Auth login scope flags. */
public enum class EosAuthScopeFlags(val value: Int) {
    NoFlags(0),
    BasicProfile(1),
    FriendsList(2),
    Presence(4),
    FriendsManagement(8);

    public companion object {
        public fun fromValue(v: Int): Set<EosAuthScopeFlags> = entries.filter { v and it.value != 0 }.toSet()
    }
}

/** Login result. */
public class LoginResult(
    public val result: EosResult,
    public val localUserId: EpicAccountId,
)

/** Login status change info. */
public class LoginStatusChangedInfo(
    public val localUserId: EpicAccountId,
    public val previousStatus: EosLoginStatus,
    public val currentStatus: EosLoginStatus,
)

internal class AuthLoginOptions(
    var credentialType: EosAuthCredentialType,
    var id: String?,
    var token: String?,
    var continueLoginToken: EpicAccountId?,
    var scopeFlags: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt32(8, credentialType.value)
        seg.setInt64(16, arena.allocCString(id).address())
        seg.setInt64(24, arena.allocCString(token).address())
        seg.setInt64(32, continueLoginToken?.raw ?: 0L)
        seg.setInt32(40, scopeFlags)
        return seg
    }

    companion object {
        const val API_LATEST = 4
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4),
        )
    }
}

internal class AuthLogoutOptions(var localUserId: EpicAccountId) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG,
        )
    }
}

internal class AuthAddNotifyLoginStatusChangedOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}
