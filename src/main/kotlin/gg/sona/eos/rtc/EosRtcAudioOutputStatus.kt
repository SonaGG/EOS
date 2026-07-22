package gg.sona.eos.rtc

/** Status of the local audio output device. */
public enum class EosRtcAudioOutputStatus(val value: Int) {
    Idle(0),
    Playing(1),
    Failed(2);

    public companion object {
        public fun fromValue(v: Int): EosRtcAudioOutputStatus = entries.firstOrNull { it.value == v } ?: Failed
    }
}
