package gg.sona.eos.rtc

import gg.sona.eos.internal.Native
import gg.sona.eos.internal.getInt32
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

private const val DEVICE_INFORMATION_SIZE = 24L

internal fun invokeHandle(platformHandle: Long): Long {
    val fn = Native.downcall(
        "EOS_Platform_GetRTCInterface",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
    )
    return fn.invokeExact(platformHandle) as Long
}

internal fun readCString(data: MemorySegment, offset: Long): String? {
    val ptr = data.get(ValueLayout.ADDRESS, offset)
    if (ptr.address() == 0L) return null
    return ptr.reinterpret(Long.MAX_VALUE).getString(0)
}

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
