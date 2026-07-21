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
package gg.sona.eos.rtc

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
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.withCallArena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Real-time communication interface. Entry point for joining and leaving
 * voice chat rooms.
 *
 * Typical use: call [joinRoom] with a room name and the auth token obtained
 * from your dedicated server (see [EosRtcAdmin.queryJoinRoomToken]). Use the
 * [audio], [data], and [admin] sub-interfaces to manage voice, data channels,
 * and server-side operations respectively.
 */
public class EosRtc internal constructor(private val platform: EosPlatform) {

    /** Audio sub-interface. */
    public val audio: EosRtcAudio = EosRtcAudio(platform)

    /** Data sub-interface for in-room data channel communication. */
    public val data: EosRtcData = EosRtcData(platform)

    /** Admin sub-interface for server-side management. */
    public val admin: EosRtcAdmin = EosRtcAdmin(platform)

    /** Internal: the C handle for the platform's RTC interface. */
    internal fun rtcHandle(): Long = invokeHandle(platform.handle)

    /**
     * Join a voice room. Lobby-managed rooms must not be joined directly; the
     * lobby system will join on the user's behalf.
     *
     * @param participantToken a token obtained from the dedicated server via
     *   [EosRtcAdmin.queryJoinRoomToken].
     */
    public fun joinRoom(
        localUserId: ProductUserId,
        roomName: String,
        clientBaseUrl: String,
        participantToken: String,
        participantId: ProductUserId? = null,
        flags: Int = 0,
        manualAudioInput: Boolean = false,
        manualAudioOutput: Boolean = false,
    ): CompletableFuture<JoinRoomResult> {
        val future = CompletableFuture<JoinRoomResult>()
        val invoker = EosCallback { data ->
            future.complete(
                JoinRoomResult(
                    RtcCallback.readResult(data),
                    RtcCallback.readLocalUserId(data),
                    roomName,
                )
            )
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcJoinRoomOptions(
            localUserId, roomName, clientBaseUrl, participantToken,
            participantId, flags, manualAudioInput, manualAudioOutput,
        )
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTC_JoinRoom",
                listOf(rtcHandle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun leaveRoom(localUserId: ProductUserId, roomName: String): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(RtcCallback.readResult(data))
        })
        val options = RtcLeaveRoomOptions(localUserId, roomName)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTC_LeaveRoom",
                listOf(rtcHandle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun blockParticipant(
        localUserId: ProductUserId,
        roomName: String,
        participantId: ProductUserId,
        blocked: Boolean,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(RtcCallback.readResult(data))
        })
        val options = RtcBlockParticipantOptions(localUserId, roomName, participantId, blocked)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTC_BlockParticipant",
                listOf(rtcHandle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun addNotifyDisconnected(
        localUserId: ProductUserId,
        roomName: String,
        callback: (DisconnectedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            callback(
                DisconnectedInfo(
                    RtcCallback.readResult(data),
                    RtcCallback.readLocalUserId(data),
                    RtcCallback.readString(data, 32) ?: "",
                )
            )
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAddNotifyDisconnectedOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTC_AddNotifyDisconnected",
                listOf(rtcHandle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyDisconnected(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTC_RemoveNotifyDisconnected",
            listOf(rtcHandle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyParticipantStatusChanged(
        localUserId: ProductUserId,
        roomName: String,
        callback: (ParticipantStatusChangedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val roomName = RtcCallback.readString(data, 24) ?: ""
            val participantId = ProductUserId(data.getInt64(32))
            val status = EosRtcParticipantStatus.fromValue(data.getInt32(40))
            val inBlocklist = data.getInt32(72) != 0
            callback(ParticipantStatusChangedInfo(localUserId, roomName, participantId, status, inBlocklist))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAddNotifyParticipantStatusChangedOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTC_AddNotifyParticipantStatusChanged",
                listOf(rtcHandle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyParticipantStatusChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTC_RemoveNotifyParticipantStatusChanged",
            listOf(rtcHandle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyRoomBeforeJoin(
        localUserId: ProductUserId,
        callback: (RoomBeforeJoinInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val roomName = RtcCallback.readString(data, 24) ?: ""
            callback(RoomBeforeJoinInfo(localUserId, roomName))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAddNotifyRoomBeforeJoinOptions(localUserId)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTC_AddNotifyRoomBeforeJoin",
                listOf(rtcHandle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyRoomBeforeJoin(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTC_RemoveNotifyRoomBeforeJoin",
            listOf(rtcHandle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyRoomStatisticsUpdated(
        localUserId: ProductUserId,
        roomName: String,
        callback: (RoomStatisticsUpdatedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val roomName = RtcCallback.readString(data, 24) ?: ""
            val stat = RtcCallback.readString(data, 32) ?: ""
            callback(RoomStatisticsUpdatedInfo(localUserId, roomName, stat))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAddNotifyRoomStatisticsUpdatedOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTC_AddNotifyRoomStatisticsUpdated",
                listOf(rtcHandle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyRoomStatisticsUpdated(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTC_RemoveNotifyRoomStatisticsUpdated",
            listOf(rtcHandle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun setSetting(name: String, value: String): EosResult {
        val options = RtcSetSettingOptions(name, value)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_RTC_SetSetting",
                    listOf(rtcHandle(), seg),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }
    }

    public fun setRoomSetting(localUserId: ProductUserId, roomName: String, name: String, value: String): EosResult {
        val options = RtcSetRoomSettingOptions(localUserId, roomName, name, value)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_RTC_SetRoomSetting",
                    listOf(rtcHandle(), seg),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }
    }
}

internal fun invokeHandle(platformHandle: Long): Long {
    val fn = Native.downcall(
        "EOS_Platform_GetRTCInterface",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
    )
    return fn.invokeExact(platformHandle) as Long
}
