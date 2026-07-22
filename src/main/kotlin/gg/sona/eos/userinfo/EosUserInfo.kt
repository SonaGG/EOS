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
package gg.sona.eos.userinfo

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.EosExternalAccountType
import gg.sona.eos.common.EosOnlinePlatform
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/** UserInfo interface for fetching user information such as display name. */
class EosUserInfo internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetUserInfoInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    fun queryUserInfo(localUserId: EpicAccountId, targetUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_UserInfo_QueryUserInfoCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = UserInfoQueryUserInfoOptions(localUserId, targetUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UserInfo_QueryUserInfo",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun queryUserInfoByDisplayName(
        localUserId: EpicAccountId,
        displayName: String
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_UserInfo_QueryUserInfoByDisplayNameCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, TargetUserId@24, DisplayName@32
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = UserInfoQueryUserInfoByDisplayNameOptions(localUserId, displayName)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UserInfo_QueryUserInfoByDisplayName",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun queryUserInfoByExternalAccount(
        localUserId: EpicAccountId,
        externalAccountType: EosExternalAccountType,
        externalAccountId: String,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_UserInfo_QueryUserInfoByExternalAccountCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, ExternalAccountId@24, AccountType@32, TargetUserId@40
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = UserInfoQueryUserInfoByExternalAccountOptions(localUserId, externalAccountType, externalAccountId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UserInfo_QueryUserInfoByExternalAccount",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun getLocalPlatformType(): EosOnlinePlatform = withCallArena { arena ->
        val options = UserInfoGetLocalPlatformTypeOptions()
        EosOnlinePlatform.fromValue(
            Native.invoke(
                "EOS_UserInfo_GetLocalPlatformType",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }
}

