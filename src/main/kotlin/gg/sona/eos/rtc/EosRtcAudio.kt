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
package gg.sona.eos.rtc

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
import gg.sona.eos.common.ProductUserId
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

/**
 * RTC audio sub-interface for managing voice capture, playback, and devices.
 */
public class EosRtcAudio internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_RTC_GetAudioInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        val rtcHandle = Native.invoke<Long>(
            "EOS_Platform_GetRTCInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
            platform.handle
        )
        return fn.invokeExact(rtcHandle) as Long
    }

    /** Send an audio frame to the room's participants. Requires manual audio input to be enabled. */
    public fun sendAudio(
        localUserId: ProductUserId,
        roomName: String,
        frames: ShortArray,
        sampleRate: Int,
        channels: Int,
    ): EosResult {
        require(frames.isNotEmpty()) { "frames must not be empty" }
        return withCallArena { arena ->
            val bufferSeg = arena.allocate(MemoryLayout.structLayout(
                ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
                ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                MemoryLayout.paddingLayout(4),
            ))
            val framesSeg = arena.allocate(ValueLayout.JAVA_SHORT, frames.size.toLong())
            framesSeg.copyFrom(MemorySegment.ofArray(frames))
            bufferSeg.setInt32(0, 1) // API version
            bufferSeg.setInt64(8, framesSeg.address())
            bufferSeg.setInt32(16, frames.size)
            bufferSeg.setInt32(20, sampleRate)
            bufferSeg.setInt32(24, channels)
            val options = RtcAudioSendAudioOptions(localUserId, roomName, bufferSeg)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_RTCAudio_SendAudio",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }
    }

    /** Update sending audio status (enabled, disabled, etc.) for a room. */
    public fun updateSending(
        localUserId: ProductUserId,
        roomName: String,
        status: EosRtcAudioStatus,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_UpdateSendingCallbackInfo: ResultCode@0
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAudioUpdateSendingOptions(localUserId, roomName, status)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateSending",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /** Update receiving audio status (per-room or per-participant). */
    public fun updateReceiving(
        localUserId: ProductUserId,
        roomName: String,
        participantId: ProductUserId?,
        audioEnabled: Boolean,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_UpdateReceivingCallbackInfo: ResultCode@0
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAudioUpdateReceivingOptions(localUserId, roomName, participantId, audioEnabled)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateReceiving",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun updateSendingVolume(
        localUserId: ProductUserId,
        roomName: String,
        volume: Float,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_UpdateSendingVolumeCallbackInfo: ResultCode@0
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAudioUpdateSendingVolumeOptions(localUserId, roomName, volume)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateSendingVolume",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun updateReceivingVolume(
        localUserId: ProductUserId,
        roomName: String,
        volume: Float,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_UpdateReceivingVolumeCallbackInfo: ResultCode@0
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAudioUpdateReceivingVolumeOptions(localUserId, roomName, volume)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateReceivingVolume",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun updateParticipantVolume(
        localUserId: ProductUserId,
        roomName: String,
        participantId: ProductUserId?,
        volume: Float,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_UpdateParticipantVolumeCallbackInfo: ResultCode@0
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAudioUpdateParticipantVolumeOptions(localUserId, roomName, participantId, volume)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateParticipantVolume",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun addNotifyParticipantUpdated(
        localUserId: ProductUserId,
        roomName: String,
        callback: (ParticipantUpdatedInfo) -> Unit,
    ): NotificationHandle {
        // EOS_RTCAudio_ParticipantUpdatedCallbackInfo: ClientData@0, LocalUserId@8, RoomName@16,
        // ParticipantId@24, bSpeaking@32, AudioStatus@36
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(8))
            val roomName = readCString(data, 16) ?: ""
            val participantId = ProductUserId(data.getInt64(24))
            val speaking = data.getInt32(32) != 0
            val audioStatus = EosRtcAudioStatus.fromValue(data.getInt32(36))
            callback(ParticipantUpdatedInfo(localUserId, roomName, participantId, speaking, audioStatus))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAudioAddNotifyParticipantUpdatedOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCAudio_AddNotifyParticipantUpdated",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    /**
     * Observes outgoing audio frames just before they are encoded and sent.
     *
     * The room's `speaking` state is reported by the RTC server, and a room configured without that
     * feature never raises it - including for the local user, whose own microphone the client is
     * holding. This is the only API that exposes the local audio itself, so it is the only way to
     * derive a speaking indicator in that configuration.
     *
     * Called on an EOS audio thread at frame cadence (typically every 10ms), so the callback must be
     * cheap and must not block. [AudioFrames] reads native memory that EOS owns and frees when the
     * callback returns - measure inside the callback and keep the number, never the buffer.
     */
    public fun addNotifyAudioBeforeSend(
        localUserId: ProductUserId,
        roomName: String,
        callback: (AudioBeforeSendInfo) -> Unit,
    ): NotificationHandle {
        // EOS_RTCAudio_AudioBeforeSendCallbackInfo: ClientData@0, LocalUserId@8, RoomName@16,
        // Buffer@24
        val invoker = EosCallback { data ->
            val user = ProductUserId(data.getInt64(8))
            val room = readCString(data, 16) ?: ""
            val bufferPtr = data.get(ValueLayout.ADDRESS, 24)

            if (bufferPtr.address() != 0L) {
                callback(AudioBeforeSendInfo(user, room, AudioFrames(bufferPtr)))
            }
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAudioAddNotifyAudioBeforeSendOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCAudio_AddNotifyAudioBeforeSend",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyAudioBeforeSend(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyAudioBeforeSend",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun removeNotifyParticipantUpdated(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyParticipantUpdated",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyAudioDevicesChanged(callback: () -> Unit): NotificationHandle {
        val invoker = EosCallback { _ -> callback() }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAudioAddNotifyAudioDevicesChangedOptions()
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCAudio_AddNotifyAudioDevicesChanged",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyAudioDevicesChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyAudioDevicesChanged",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyAudioInputState(
        localUserId: ProductUserId,
        roomName: String,
        callback: (AudioInputStateInfo) -> Unit,
    ): NotificationHandle {
        // EOS_RTCAudio_AudioInputStateCallbackInfo: ClientData@0, LocalUserId@8, RoomName@16, Status@24
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(8))
            val roomName = readCString(data, 16) ?: ""
            val status = EosRtcAudioInputStatus.fromValue(data.getInt32(24))
            callback(AudioInputStateInfo(localUserId, roomName, status))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAudioAddNotifyAudioInputStateOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCAudio_AddNotifyAudioInputState",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyAudioInputState(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyAudioInputState",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyAudioOutputState(
        localUserId: ProductUserId,
        roomName: String,
        callback: (AudioOutputStateInfo) -> Unit,
    ): NotificationHandle {
        // EOS_RTCAudio_AudioOutputStateCallbackInfo: ClientData@0, LocalUserId@8, RoomName@16, Status@24
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(8))
            val roomName = readCString(data, 16) ?: ""
            val status = EosRtcAudioOutputStatus.fromValue(data.getInt32(24))
            callback(AudioOutputStateInfo(localUserId, roomName, status))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAudioAddNotifyAudioOutputStateOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCAudio_AddNotifyAudioOutputState",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyAudioOutputState(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyAudioOutputState",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun registerPlatformUser(
        platformUserId: String,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_OnRegisterPlatformUserCallbackInfo: ResultCode@0
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAudioRegisterPlatformUserOptions(platformUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_RegisterPlatformUser",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun unregisterPlatformUser(platformUserId: String): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_OnUnregisterPlatformUserCallbackInfo: ResultCode@0
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAudioUnregisterPlatformUserOptions(platformUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UnregisterPlatformUser",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    // region Audio devices

    /**
     * Refresh the SDK's cached list of audio input devices. The cache is only
     * populated once this completes; read it with [getInputDevicesCount] and
     * [copyInputDeviceInformationByIndex].
     */
    public fun queryInputDevicesInformation(): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_OnQueryInputDevicesInformationCallbackInfo: ResultCode@0
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        withCallArena { arena ->
            val seg = RtcAudioQueryDevicesInformationOptions().writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_QueryInputDevicesInformation",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /** Number of cached audio input devices. Call [queryInputDevicesInformation] first. */
    public fun getInputDevicesCount(): Int = withCallArena { arena ->
        val seg = RtcAudioGetDevicesCountOptions().writeTo(arena)
        Native.invoke(
            "EOS_RTCAudio_GetInputDevicesCount",
            listOf(handle(), seg),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            ValueLayout.JAVA_INT,
        ) as Int
    }

    /** Copy the cached information for the input device at [deviceIndex], or null if unavailable. */
    public fun copyInputDeviceInformationByIndex(deviceIndex: Int): EosRtcAudioDeviceInfo? =
        withCallArena { arena ->
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_RTCAudio_CopyInputDeviceInformationByIndex",
                    listOf(
                        handle(),
                        RtcAudioCopyDeviceInformationByIndexOptions(deviceIndex).writeTo(arena),
                        outPtr,
                    ),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val info = readDeviceInformation(seg)
            val releaseFn = Native.downcall(
                "EOS_RTCAudio_InputDeviceInformation_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            )
            releaseFn.invokeExact(seg)
            info
        }

    /**
     * Refresh the SDK's cached list of audio output devices. The cache is only
     * populated once this completes; read it with [getOutputDevicesCount] and
     * [copyOutputDeviceInformationByIndex].
     */
    public fun queryOutputDevicesInformation(): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_OnQueryOutputDevicesInformationCallbackInfo: ResultCode@0
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        withCallArena { arena ->
            val seg = RtcAudioQueryDevicesInformationOptions().writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_QueryOutputDevicesInformation",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /** Number of cached audio output devices. Call [queryOutputDevicesInformation] first. */
    public fun getOutputDevicesCount(): Int = withCallArena { arena ->
        val seg = RtcAudioGetDevicesCountOptions().writeTo(arena)
        Native.invoke(
            "EOS_RTCAudio_GetOutputDevicesCount",
            listOf(handle(), seg),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            ValueLayout.JAVA_INT,
        ) as Int
    }

    /** Copy the cached information for the output device at [deviceIndex], or null if unavailable. */
    public fun copyOutputDeviceInformationByIndex(deviceIndex: Int): EosRtcAudioDeviceInfo? =
        withCallArena { arena ->
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_RTCAudio_CopyOutputDeviceInformationByIndex",
                    listOf(
                        handle(),
                        RtcAudioCopyDeviceInformationByIndexOptions(deviceIndex).writeTo(arena),
                        outPtr,
                    ),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val info = readDeviceInformation(seg)
            val releaseFn = Native.downcall(
                "EOS_RTCAudio_OutputDeviceInformation_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            )
            releaseFn.invokeExact(seg)
            info
        }

    /**
     * Select the audio input device for [localUserId]. Pass a null or blank
     * [realDeviceId] to fall back to the system default. An unknown id is
     * remembered and applied if that device later appears.
     */
    public fun setInputDeviceSettings(
        localUserId: ProductUserId,
        realDeviceId: String?,
        platformAec: Boolean = false,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_OnSetInputDeviceSettingsCallbackInfo: ResultCode@0
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAudioSetInputDeviceSettingsOptions(localUserId, realDeviceId, platformAec)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_SetInputDeviceSettings",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Select the audio output device for [localUserId]. Pass a null or blank
     * [realDeviceId] to fall back to the system default.
     */
    public fun setOutputDeviceSettings(
        localUserId: ProductUserId,
        realDeviceId: String?,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_RTCAudio_OnSetOutputDeviceSettingsCallbackInfo: ResultCode@0
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcAudioSetOutputDeviceSettingsOptions(localUserId, realDeviceId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_SetOutputDeviceSettings",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    // endregion
}

/** RTC audio status of a participant or a room. */
public enum class EosRtcAudioStatus(val value: Int) {
    Unsupported(0),
    Enabled(1),
    Disabled(2),
    AdminDisabled(3),
    NotListeningDisabled(4);

    public companion object {
        public fun fromValue(v: Int): EosRtcAudioStatus = entries.firstOrNull { it.value == v } ?: Unsupported
    }
}

/** Status of the local audio input device. */
public enum class EosRtcAudioInputStatus(val value: Int) {
    Idle(0),
    Recording(1),
    RecordingSilent(2),
    RecordingDisconnected(3),
    Failed(4);

    public companion object {
        public fun fromValue(v: Int): EosRtcAudioInputStatus = entries.firstOrNull { it.value == v } ?: Failed
    }
}

/** Status of the local audio output device. */
public enum class EosRtcAudioOutputStatus(val value: Int) {
    Idle(0),
    Playing(1),
    Failed(2);

    public companion object {
        public fun fromValue(v: Int): EosRtcAudioOutputStatus = entries.firstOrNull { it.value == v } ?: Failed
    }
}

/** Participant audio update info. */
public class ParticipantUpdatedInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val participantId: ProductUserId,
    public val speaking: Boolean,
    public val audioStatus: EosRtcAudioStatus,
)

/**
 * A window onto the outgoing audio buffer, valid only for the duration of the callback.
 *
 * Wraps EOS-owned memory rather than copying it: this arrives every 10ms per room, and copying a
 * frame each time would allocate roughly 100 short arrays a second for a measurement that collapses
 * to a single float. Nothing here retains the segment beyond the call, and neither should callers -
 * read what you need, keep the number, drop the buffer.
 *
 * Corresponds to `EOS_RTCAudio_AudioBuffer`: ApiVersion@0, Frames@8, FramesCount@16, SampleRate@20,
 * Channels@24.
 */
public class AudioFrames internal constructor(bufferPtr: MemorySegment) {
    private val buffer: MemorySegment = bufferPtr.reinterpret(32)

    /** Frames per channel, not the total sample count. */
    public val framesCount: Int get() = buffer.getInt32(16)

    public val sampleRate: Int get() = buffer.getInt32(20)

    public val channels: Int get() = buffer.getInt32(24)

    /**
     * Root-mean-square amplitude over the whole buffer, normalised to 0.0..1.0.
     *
     * RMS rather than peak: peak reacts to a single click or a keyboard knock, where RMS tracks
     * sustained energy, which is what distinguishes speech from room noise.
     */
    public fun rms(): Float {
        val frames = buffer.get(ValueLayout.ADDRESS, 8)
        val samples = framesCount * channels

        if (frames.address() == 0L || samples <= 0) return 0f

        val pcm = frames.reinterpret(samples * 2L)
        var sum = 0.0

        for (i in 0 until samples) {
            val sample = pcm.get(ValueLayout.JAVA_SHORT, i * 2L).toDouble()
            sum += sample * sample
        }

        // Short.MAX_VALUE normalises full-scale PCM16 to 1.0.
        return (kotlin.math.sqrt(sum / samples) / Short.MAX_VALUE).toFloat()
    }
}

/** Outgoing audio, delivered just before encoding. */
public class AudioBeforeSendInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val buffer: AudioFrames,
)

/** Local audio input state change info. */
public class AudioInputStateInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val status: EosRtcAudioInputStatus,
)

/** Local audio output state change info. */
public class AudioOutputStateInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val status: EosRtcAudioOutputStatus,
)

internal fun readCString(data: MemorySegment, offset: Long): String? {
    val ptr = data.get(ValueLayout.ADDRESS, offset)
    if (ptr.address() == 0L) return null
    return ptr.reinterpret(Long.MAX_VALUE).getString(0)
}

internal class RtcAudioSendAudioOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var buffer: MemorySegment,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt64(24, buffer.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
        )
    }
}

internal class RtcAudioUpdateSendingOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var status: EosRtcAudioStatus,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt32(24, status.value)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class RtcAudioUpdateReceivingOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var participantId: ProductUserId?,
    var audioEnabled: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt64(24, participantId?.raw ?: 0L)
        seg.setInt32(32, if (audioEnabled) 1 else 0)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_INT,
        )
    }
}

internal class RtcAudioUpdateSendingVolumeOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var volume: Float,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setFloat(24, volume)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT,
            MemoryLayout.paddingLayout(4),
        )
    }
}

internal class RtcAudioUpdateReceivingVolumeOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var volume: Float,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setFloat(24, volume)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_FLOAT,
            MemoryLayout.paddingLayout(4),
        )
    }
}

internal class RtcAudioUpdateParticipantVolumeOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var participantId: ProductUserId?,
    var volume: Float,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt64(24, participantId?.raw ?: 0L)
        seg.setFloat(32, volume)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
            ValueLayout.JAVA_FLOAT,
        )
    }
}

internal class RtcAudioAddNotifyAudioBeforeSendOptions(
    var localUserId: ProductUserId,
    var roomName: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class RtcAudioAddNotifyParticipantUpdatedOptions(
    var localUserId: ProductUserId,
    var roomName: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class RtcAudioAddNotifyAudioDevicesChangedOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class RtcAudioAddNotifyAudioInputStateOptions(
    var localUserId: ProductUserId,
    var roomName: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class RtcAudioAddNotifyAudioOutputStateOptions(
    var localUserId: ProductUserId,
    var roomName: String,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}

internal class RtcAudioRegisterPlatformUserOptions(var platformUserId: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, arena.allocCString(platformUserId).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}

internal class RtcAudioUnregisterPlatformUserOptions(var platformUserId: String) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, arena.allocCString(platformUserId).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.ADDRESS,
        )
    }
}

/** An audio input or output device reported by the RTC audio interface. */
public class EosRtcAudioDeviceInfo(
    public val deviceId: String,
    public val deviceName: String,
    public val isDefault: Boolean,
)

/**
 * Reads an EOS_RTCAudio_InputDeviceInformation / EOS_RTCAudio_OutputDeviceInformation.
 * Both share the layout ApiVersion@0, bDefaultDevice@4, DeviceId@8, DeviceName@16.
 */
internal fun readDeviceInformation(seg: MemorySegment): EosRtcAudioDeviceInfo {
    val view = seg.reinterpret(DEVICE_INFORMATION_SIZE)
    return EosRtcAudioDeviceInfo(
        deviceId = readCString(view, 8) ?: "",
        deviceName = readCString(view, 16) ?: "",
        isDefault = view.getInt32(4) != 0,
    )
}

private const val DEVICE_INFORMATION_SIZE = 24L

/** Shared by EOS_RTCAudio_Query{Input,Output}DevicesInformationOptions - ApiVersion only. */
internal class RtcAudioQueryDevicesInformationOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(ValueLayout.JAVA_INT)
    }
}

/** Shared by EOS_RTCAudio_Get{Input,Output}DevicesCountOptions - ApiVersion only. */
internal class RtcAudioGetDevicesCountOptions : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(ValueLayout.JAVA_INT)
    }
}

/** Shared by EOS_RTCAudio_Copy{Input,Output}DeviceInformationByIndexOptions. */
internal class RtcAudioCopyDeviceInformationByIndexOptions(
    var deviceIndex: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt32(4, deviceIndex)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        )
    }
}

internal class RtcAudioSetInputDeviceSettingsOptions(
    var localUserId: ProductUserId,
    var realDeviceId: String?,
    var platformAec: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(realDeviceId?.ifBlank { null }).address())
        seg.setInt32(24, if (platformAec) 1 else 0)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
        )
    }
}

internal class RtcAudioSetOutputDeviceSettingsOptions(
    var localUserId: ProductUserId,
    var realDeviceId: String?,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(realDeviceId?.ifBlank { null }).address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        )
    }
}
