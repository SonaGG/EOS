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
package gg.sona.eos.ui

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * UI interface. Provides access to the social overlay (friends list, chat)
 * and key/button bindings.
 */
class EosUi internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetUIInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /** Open the social overlay's friends list. */
    fun showFriends(localUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_UI_ShowFriendsCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = UiShowFriendsOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UI_ShowFriends",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /** Hide the social overlay. */
    fun hideFriends(localUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_UI_HideFriendsCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = UiHideFriendsOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UI_HideFriends",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun isFriendsOverlayVisible(localUserId: EpicAccountId): Boolean {
        val options = UiGetFriendsVisibleOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            (Native.invoke(
                "EOS_UI_GetFriendsVisible",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int) != 0
        }
    }

    fun isFriendsOverlayExclusiveInput(localUserId: EpicAccountId): Boolean {
        val options = UiGetFriendsExclusiveInputOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            (Native.invoke(
                "EOS_UI_GetFriendsExclusiveInput",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int) != 0
        }
    }

    fun addNotifyDisplaySettingsUpdated(
        callback: (DisplaySettingsUpdatedInfo) -> Unit,
    ): NotificationHandle {
        // EOS_UI_OnDisplaySettingsUpdatedCallbackInfo: ClientData@0, bIsVisible@8, bIsExclusiveInput@12
        val invoker = EosCallback { data ->
            val visible = data.getInt32(8) != 0
            val exclusiveInput = data.getInt32(12) != 0
            callback(DisplaySettingsUpdatedInfo(visible, exclusiveInput))
        }
        val handle = CallbackStubs.register(invoker)
        val options = UiAddNotifyDisplaySettingsUpdatedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_UI_AddNotifyDisplaySettingsUpdated",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    fun removeNotifyDisplaySettingsUpdated(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_UI_RemoveNotifyDisplaySettingsUpdated",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    fun setToggleFriendsKey(keyCombination: Int): EosResult = withCallArena { arena ->
        val options = UiSetToggleFriendsKeyOptions(keyCombination)
        EosResult.fromValue(
            Native.invoke(
                "EOS_UI_SetToggleFriendsKey",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    fun getToggleFriendsKey(): Int = withCallArena { arena ->
        val options = UiGetToggleFriendsKeyOptions()
        Native.invoke(
            "EOS_UI_GetToggleFriendsKey",
            listOf(handle(), options.writeTo(arena)),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            ValueLayout.JAVA_INT,
        ) as Int
    }

    fun isValidKeyCombination(keyCombination: Int): Boolean = withCallArena { arena ->
        (Native.invoke(
            "EOS_UI_IsValidKeyCombination",
            listOf(handle(), keyCombination),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT),
            ValueLayout.JAVA_INT,
        ) as Int) != 0
    }

    fun setToggleFriendsButton(buttonFlags: Int): EosResult = withCallArena { arena ->
        val options = UiSetToggleFriendsButtonOptions(buttonFlags)
        EosResult.fromValue(
            Native.invoke(
                "EOS_UI_SetToggleFriendsButton",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    fun getToggleFriendsButton(): Int = withCallArena { arena ->
        val options = UiGetToggleFriendsButtonOptions()
        Native.invoke(
            "EOS_UI_GetToggleFriendsButton",
            listOf(handle(), options.writeTo(arena)),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            ValueLayout.JAVA_INT,
        ) as Int
    }

    fun isValidButtonCombination(flags: Int): Boolean = withCallArena { arena ->
        (Native.invoke(
            "EOS_UI_IsValidButtonCombination",
            listOf(handle(), flags),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT),
            ValueLayout.JAVA_INT,
        ) as Int) != 0
    }

    fun setDisplayPreference(preference: EosUiNotificationLocation): EosResult =
        withCallArena { arena ->
            val options = UiSetDisplayPreferenceOptions(preference)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_UI_SetDisplayPreference",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }

    fun getNotificationLocationPreference(): EosUiNotificationLocation = withCallArena { arena ->
        EosUiNotificationLocation.fromValue(
            Native.invoke(
                "EOS_UI_GetNotificationLocationPreference",
                listOf(handle()),
                listOf(ValueLayout.JAVA_LONG),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    /** Acknowledge that the UI has handled the given event id. */
    fun acknowledgeEventId(eventId: Long): EosResult = withCallArena { arena ->
        val options = UiAcknowledgeEventIdOptions(eventId)
        EosResult.fromValue(
            Native.invoke(
                "EOS_UI_AcknowledgeEventId",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }

    /** Reports current gamepad input state to the EOS overlay. */
    fun reportInputState(
        localUserId: EpicAccountId,
        buttonFlags: Int,
    ): EosResult = withCallArena { arena ->
        val options = UiReportInputStateOptions(localUserId, buttonFlags)
        EosResult.fromValue(
            Native.invoke(
                "EOS_UI_ReportInputState",
                listOf(handle(), options.writeTo(arena)),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        )
    }
}
