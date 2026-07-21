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
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = RtcAudioUpdateSendingOptions(localUserId, roomName, status)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateSending",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = RtcAudioUpdateReceivingOptions(localUserId, roomName, participantId, audioEnabled)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateReceiving",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = RtcAudioUpdateSendingVolumeOptions(localUserId, roomName, volume)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateSendingVolume",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = RtcAudioUpdateReceivingVolumeOptions(localUserId, roomName, volume)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateReceivingVolume",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = RtcAudioUpdateParticipantVolumeOptions(localUserId, roomName, participantId, volume)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UpdateParticipantVolume",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun addNotifyParticipantUpdated(
        localUserId: ProductUserId,
        roomName: String,
        callback: (ParticipantUpdatedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val roomName = readCString(data, 24) ?: ""
            val participantId = ProductUserId(data.getInt64(32))
            val speaking = data.getInt32(40) != 0
            val audioStatus = EosRtcAudioStatus.fromValue(data.getInt32(44))
            callback(ParticipantUpdatedInfo(localUserId, roomName, participantId, speaking, audioStatus))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAudioAddNotifyParticipantUpdatedOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCAudio_AddNotifyParticipantUpdated",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
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
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val roomName = readCString(data, 24) ?: ""
            val status = EosRtcAudioInputStatus.fromValue(data.getInt32(32))
            callback(AudioInputStateInfo(localUserId, roomName, status))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAudioAddNotifyAudioInputStateOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCAudio_AddNotifyAudioInputState",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val roomName = readCString(data, 24) ?: ""
            val status = EosRtcAudioOutputStatus.fromValue(data.getInt32(32))
            callback(AudioOutputStateInfo(localUserId, roomName, status))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcAudioAddNotifyAudioOutputStateOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCAudio_AddNotifyAudioOutputState",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = RtcAudioRegisterPlatformUserOptions(platformUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_RegisterPlatformUser",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun unregisterPlatformUser(platformUserId: String): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val handle = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = RtcAudioUnregisterPlatformUserOptions(platformUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCAudio_UnregisterPlatformUser",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }
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
