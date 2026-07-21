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

import gg.sona.eos.EosPlatform
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
 * RTC Data sub-interface for sending and receiving arbitrary in-room data.
 *
 * The Data channel must be enabled when joining a room by setting
 * [EosRtcFlag.EnableDataChannel] in the [EosRtc.joinRoom] flags.
 */
public class EosRtcData internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_RTC_GetDataInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        val rtcHandle = Native.invoke<Long>(
            "EOS_Platform_GetRTCInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
            platform.handle
        )
        return fn.invokeExact(rtcHandle) as Long
    }

    /** Send arbitrary data to all participants in a room. */
    public fun sendData(
        localUserId: ProductUserId,
        roomName: String,
        data: ByteArray,
    ): EosResult {
        require(data.size <= MAX_PACKET_SIZE) { "RTC data packet exceeds maximum size of $MAX_PACKET_SIZE" }
        return withCallArena { arena ->
            val bytes = arena.allocate(data.size.toLong())
            bytes.copyFrom(MemorySegment.ofArray(data))
            val options = RtcDataSendDataOptions(localUserId, roomName, bytes, data.size)
            EosResult.fromValue(
                Native.invoke(
                    "EOS_RTCData_SendData",
                    listOf(handle(), options.writeTo(arena)),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
        }
    }

    public fun updateSending(
        localUserId: ProductUserId,
        roomName: String,
        dataEnabled: Boolean,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = RtcDataUpdateSendingOptions(localUserId, roomName, dataEnabled)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCData_UpdateSending",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun updateReceiving(
        localUserId: ProductUserId,
        roomName: String,
        participantId: ProductUserId?,
        dataEnabled: Boolean,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(8)))
        })
        val options = RtcDataUpdateReceivingOptions(localUserId, roomName, participantId, dataEnabled)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCData_UpdateReceiving",
                listOf(handle(), seg, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun addNotifyDataReceived(
        localUserId: ProductUserId,
        roomName: String,
        callback: (DataReceivedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val roomName = readCString(data, 24) ?: ""
            val len = data.getInt32(32) and 0x7fffffff
            val dataPtr = data.get(ValueLayout.ADDRESS, 40)
            val bytes = if (dataPtr.address() == 0L || len == 0) ByteArray(0)
            else {
                val arr = ByteArray(len)
                MemorySegment.ofArray(arr).copyFrom(MemorySegment.ofAddress(dataPtr.address()).reinterpret(len.toLong()))
                arr
            }
            val sender = ProductUserId(data.getInt64(48))
            callback(DataReceivedInfo(localUserId, roomName, bytes, sender))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcDataAddNotifyDataReceivedOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCData_AddNotifyDataReceived",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyDataReceived(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCData_RemoveNotifyDataReceived",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public fun addNotifyParticipantUpdated(
        localUserId: ProductUserId,
        roomName: String,
        callback: (RtcDataParticipantUpdatedInfo) -> Unit,
    ): NotificationHandle {
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(16))
            val roomName = readCString(data, 24) ?: ""
            val participantId = ProductUserId(data.getInt64(32))
            val status = EosRtcDataStatus.fromValue(data.getInt32(40))
            callback(RtcDataParticipantUpdatedInfo(localUserId, roomName, participantId, status))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcDataAddNotifyParticipantUpdatedOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCData_AddNotifyParticipantUpdated",
                listOf(handle(), seg, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                ValueLayout.JAVA_LONG,
            ) as Long
        }
        return NotificationHandle(notifId, handle.id)
    }

    public fun removeNotifyParticipantUpdated(handle: NotificationHandle) {
        Native.invokeVoid(
            "EOS_RTCData_RemoveNotifyParticipantUpdated",
            listOf(handle(), handle.notificationId),
            listOf(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        CallbackStubs.release(handle.callbackId)
    }

    public companion object {
        public const val MAX_PACKET_SIZE: Int = 1170
    }
}

public enum class EosRtcDataStatus(val value: Int) {
    Unsupported(0),
    Enabled(1),
    Disabled(2);

    public companion object {
        public fun fromValue(v: Int): EosRtcDataStatus = entries.firstOrNull { it.value == v } ?: Unsupported
    }
}

public class DataReceivedInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val data: ByteArray,
    public val sender: ProductUserId,
)

public class RtcDataParticipantUpdatedInfo(
    public val localUserId: ProductUserId,
    public val roomName: String,
    public val participantId: ProductUserId,
    public val status: EosRtcDataStatus,
)

internal class RtcDataSendDataOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var data: MemorySegment,
    var dataLengthBytes: Int,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt32(24, dataLengthBytes)
        seg.setInt64(32, data.address())
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
            MemoryLayout.paddingLayout(4), ValueLayout.ADDRESS,
        )
    }
}

internal class RtcDataUpdateSendingOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var dataEnabled: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt32(24, if (dataEnabled) 1 else 0)
        return seg
    }

    companion object {
        val LAYOUT: MemoryLayout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT, MemoryLayout.paddingLayout(4),
            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
        )
    }
}

internal class RtcDataUpdateReceivingOptions(
    var localUserId: ProductUserId,
    var roomName: String,
    var participantId: ProductUserId?,
    var dataEnabled: Boolean,
) : StructWriter {
    override fun writeTo(arena: Arena): MemorySegment {
        val seg = arena.allocate(LAYOUT)
        seg.setInt32(0, 1)
        seg.setInt64(8, localUserId.raw)
        seg.setInt64(16, arena.allocCString(roomName).address())
        seg.setInt64(24, participantId?.raw ?: 0L)
        seg.setInt32(32, if (dataEnabled) 1 else 0)
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

internal class RtcDataAddNotifyDataReceivedOptions(
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

internal class RtcDataAddNotifyParticipantUpdatedOptions(
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
