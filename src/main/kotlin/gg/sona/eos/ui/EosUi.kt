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
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.StructWriter
import gg.sona.eos.internal.allocCString
import gg.sona.eos.internal.getInt32
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
 * UI interface. Provides access to the social overlay (friends list, chat)
 * and key/button bindings.
 */
public class EosUi internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetUIInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    /** Open the social overlay's friends list. */
    public fun showFriends(localUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = UiShowFriendsOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UI_ShowFriends",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /** Hide the social overlay. */
    public fun hideFriends(localUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = UiHideFriendsOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_UI_HideFriends",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun isFriendsOverlayVisible(localUserId: EpicAccountId): Boolean {
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

    public fun isFriendsOverlayExclusiveInput(localUserId: EpicAccountId): Boolean {
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

    public fun addNotifyDisplaySettingsUpdated(
        callback: (DisplaySettingsUpdatedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val visible = data.getInt32(16) != 0
            val exclusiveInput = data.getInt32(20) != 0
            callback(DisplaySettingsUpdatedInfo(visible, exclusiveInput))
        }
        val handle = CallbackStubs.register(invoker)
        val options = UiAddNotifyDisplaySettingsUpdatedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_UI_AddNotifyDisplaySettingsUpdated",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyDisplaySettingsUpdated(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_UI_RemoveNotifyDisplaySettingsUpdated",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun setToggleFriendsKey(keyCombination: Int): EosResult = withCallArena { arena ->
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

    public fun getToggleFriendsKey(): Int = withCallArena { arena ->
        val options = UiGetToggleFriendsKeyOptions()
        Native.invoke(
            "EOS_UI_GetToggleFriendsKey",
            listOf(handle(), options.writeTo(arena)),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            ValueLayout.JAVA_INT,
        ) as Int
    }

    public fun isValidKeyCombination(keyCombination: Int): Boolean = withCallArena { arena ->
        (Native.invoke(
            "EOS_UI_IsValidKeyCombination",
            listOf(handle(), keyCombination),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT),
            ValueLayout.JAVA_INT,
        ) as Int) != 0
    }

    public fun setToggleFriendsButton(buttonFlags: Int): EosResult = withCallArena { arena ->
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

    public fun getToggleFriendsButton(): Int = withCallArena { arena ->
        val options = UiGetToggleFriendsButtonOptions()
        Native.invoke(
            "EOS_UI_GetToggleFriendsButton",
            listOf(handle(), options.writeTo(arena)),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            ValueLayout.JAVA_INT,
        ) as Int
    }

    public fun isValidButtonCombination(flags: Int): Boolean = withCallArena { arena ->
        (Native.invoke(
            "EOS_UI_IsValidButtonCombination",
            listOf(handle(), flags),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT),
            ValueLayout.JAVA_INT,
        ) as Int) != 0
    }

    public fun setDisplayPreference(preference: EosUiNotificationLocation): EosResult =
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

    public fun getNotificationLocationPreference(): EosUiNotificationLocation = withCallArena { arena ->
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
    public fun acknowledgeEventId(eventId: Long): EosResult = withCallArena { arena ->
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
    public fun reportInputState(
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

/** Where the social overlay places notification toasts. */
public enum class EosUiNotificationLocation(val value: Int) {
    None(0),
    TopLeft(1),
    TopRight(2),
    BottomLeft(3),
    BottomRight(4);

    public companion object {
        internal fun fromValue(v: Int): EosUiNotificationLocation =
            entries.firstOrNull { it.value == v } ?: None
    }
}

public class DisplaySettingsUpdatedInfo(
    public val friendsOverlayVisible: Boolean,
    public val friendsOverlayExclusiveInput: Boolean,
)

// region Struct writers

internal class UiShowFriendsOptions(var localUserId: EpicAccountId) : StructWriter {
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

internal class UiHideFriendsOptions(var localUserId: EpicAccountId) : StructWriter {
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

internal class UiGetFriendsVisibleOptions(var localUserId: EpicAccountId) : StructWriter {
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

internal class UiGetFriendsExclusiveInputOptions(var localUserId: EpicAccountId) : StructWriter {
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

internal class UiAddNotifyDisplaySettingsUpdatedOptions : StructWriter {
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

internal class UiSetToggleFriendsKeyOptions(var keyCombination: Int) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt32(8, keyCombination)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_INT
        )
    }
}

internal class UiGetToggleFriendsKeyOptions : StructWriter {
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

internal class UiSetToggleFriendsButtonOptions(var buttonFlags: Int) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt32(8, buttonFlags)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_INT
        )
    }
}

internal class UiGetToggleFriendsButtonOptions : StructWriter {
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

internal class UiSetDisplayPreferenceOptions(
    var preference: EosUiNotificationLocation,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt32(8, preference.value)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_INT
        )
    }
}

internal class UiAcknowledgeEventIdOptions(var eventId: Long) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, eventId)
        return seg
    }

    companion object {
        const val API_LATEST = 1
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4), ValueLayout.JAVA_LONG
        )
    }
}

internal class UiReportInputStateOptions(
    var localUserId: EpicAccountId,
    var buttonFlags: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, API_LATEST)
        seg.setInt64(8, localUserId.raw)
        seg.setInt32(16, buttonFlags)
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

// endregion
