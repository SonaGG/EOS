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
        // EOS_RTCData_UpdateSendingCallbackInfo: ResultCode@0
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcDataUpdateSendingOptions(localUserId, roomName, dataEnabled)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCData_UpdateSending",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
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
        // EOS_RTCData_UpdateReceivingCallbackInfo: ResultCode@0
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = RtcDataUpdateReceivingOptions(localUserId, roomName, participantId, dataEnabled)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_RTCData_UpdateReceiving",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    public fun addNotifyDataReceived(
        localUserId: ProductUserId,
        roomName: String,
        callback: (DataReceivedInfo) -> Unit,
    ): NotificationHandle {
        // EOS_RTCData_DataReceivedCallbackInfo: ClientData@0, LocalUserId@8, RoomName@16,
        // DataLengthBytes@24, Data@32, ParticipantId@40
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(8))
            val roomName = readCString(data, 16) ?: ""
            val len = data.getInt32(24) and 0x7fffffff
            val dataPtr = data.get(ValueLayout.ADDRESS, 32)
            val bytes = if (dataPtr.address() == 0L || len == 0) ByteArray(0)
            else {
                val arr = ByteArray(len)
                MemorySegment.ofArray(arr).copyFrom(MemorySegment.ofAddress(dataPtr.address()).reinterpret(len.toLong()))
                arr
            }
            val sender = ProductUserId(data.getInt64(40))
            callback(DataReceivedInfo(localUserId, roomName, bytes, sender))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcDataAddNotifyDataReceivedOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCData_AddNotifyDataReceived",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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
        // EOS_RTCData_ParticipantUpdatedCallbackInfo: ClientData@0, LocalUserId@8, RoomName@16,
        // ParticipantId@24, DataStatus@32
        val invoker = EosCallback { data ->
            val localUserId = ProductUserId(data.getInt64(8))
            val roomName = readCString(data, 16) ?: ""
            val participantId = ProductUserId(data.getInt64(24))
            val status = EosRtcDataStatus.fromValue(data.getInt32(32))
            callback(RtcDataParticipantUpdatedInfo(localUserId, roomName, participantId, status))
        }
        val handle = CallbackStubs.register(invoker)
        val options = RtcDataAddNotifyParticipantUpdatedOptions(localUserId, roomName)
        val notifId = withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_RTCData_AddNotifyParticipantUpdated",
                listOf(handle(), seg, MemorySegment.NULL, handle.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
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