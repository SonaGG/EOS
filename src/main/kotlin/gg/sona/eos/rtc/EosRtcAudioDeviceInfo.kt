package gg.sona.eos.rtc

/** An audio input or output device reported by the RTC audio interface. */
public class EosRtcAudioDeviceInfo(
    public val deviceId: String,
    public val deviceName: String,
    public val isDefault: Boolean,
)
