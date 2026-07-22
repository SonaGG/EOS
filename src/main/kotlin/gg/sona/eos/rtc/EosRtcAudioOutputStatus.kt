package gg.sona.eos.rtc

/** Status of the local audio output device. */
enum class EosRtcAudioOutputStatus(val value: Int) {
    Idle(0),
    Playing(1),
    Failed(2);

    companion object {
        fun fromValue(v: Int): EosRtcAudioOutputStatus = entries.firstOrNull { it.value == v } ?: Failed
    }
}
