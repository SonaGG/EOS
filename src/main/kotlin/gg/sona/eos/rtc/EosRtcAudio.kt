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

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.NotificationHandle
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * RTC audio sub-interface for managing voice capture, playback, and devices.
 */
class EosRtcAudio internal constructor(private val platform: EosPlatform) {

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
    fun sendAudio(
        localUserId: ProductUserId,
        roomName: String,
        frames: ShortArray,
        sampleRate: Int,
        channels: Int,
    ): EosResult {
        require(frames.isNotEmpty()) { "frames must not be empty" }
        return withCallArena { arena ->
            val bufferSeg = arena.allocate(
                MemoryLayout.structLayout(
                    ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                    MemoryLayout.paddingLayout(4),
                )
            )
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
    fun updateSending(
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
    fun updateReceiving(
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

    fun updateSendingVolume(
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

    fun updateReceivingVolume(
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

    fun updateParticipantVolume(
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

    fun addNotifyParticipantUpdated(
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
    fun addNotifyAudioBeforeSend(
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

    fun removeNotifyAudioBeforeSend(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyAudioBeforeSend",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    fun removeNotifyParticipantUpdated(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyParticipantUpdated",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    fun addNotifyAudioDevicesChanged(callback: () -> Unit): NotificationHandle {
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

    fun removeNotifyAudioDevicesChanged(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyAudioDevicesChanged",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    fun addNotifyAudioInputState(
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

    fun removeNotifyAudioInputState(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyAudioInputState",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    fun addNotifyAudioOutputState(
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

    fun removeNotifyAudioOutputState(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCAudio_RemoveNotifyAudioOutputState",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    fun registerPlatformUser(
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

    fun unregisterPlatformUser(platformUserId: String): CompletableFuture<EosResult> {
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
    fun queryInputDevicesInformation(): CompletableFuture<EosResult> {
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
    fun getInputDevicesCount(): Int = withCallArena { arena ->
        val seg = RtcAudioGetDevicesCountOptions().writeTo(arena)
        Native.invoke(
            "EOS_RTCAudio_GetInputDevicesCount",
            listOf(handle(), seg),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            ValueLayout.JAVA_INT,
        ) as Int
    }

    /** Copy the cached information for the input device at [deviceIndex], or null if unavailable. */
    fun copyInputDeviceInformationByIndex(deviceIndex: Int): EosRtcAudioDeviceInfo? =
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
    fun queryOutputDevicesInformation(): CompletableFuture<EosResult> {
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
    fun getOutputDevicesCount(): Int = withCallArena { arena ->
        val seg = RtcAudioGetDevicesCountOptions().writeTo(arena)
        Native.invoke(
            "EOS_RTCAudio_GetOutputDevicesCount",
            listOf(handle(), seg),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
            ValueLayout.JAVA_INT,
        ) as Int
    }

    /** Copy the cached information for the output device at [deviceIndex], or null if unavailable. */
    fun copyOutputDeviceInformationByIndex(deviceIndex: Int): EosRtcAudioDeviceInfo? =
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
    fun setInputDeviceSettings(
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
    fun setOutputDeviceSettings(
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