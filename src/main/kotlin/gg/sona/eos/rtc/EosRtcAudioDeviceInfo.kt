package gg.sona.eos.rtc

/** An audio input or output device reported by the RTC audio interface. */
class EosRtcAudioDeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val isDefault: Boolean,
)
