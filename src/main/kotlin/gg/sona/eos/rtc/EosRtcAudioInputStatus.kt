package gg.sona.eos.rtc

/** Status of the local audio input device. */
enum class EosRtcAudioInputStatus(val value: Int) {
    Idle(0),
    Recording(1),
    RecordingSilent(2),
    RecordingDisconnected(3),
    Failed(4);

    companion object {
        fun fromValue(v: Int): EosRtcAudioInputStatus = entries.firstOrNull { it.value == v } ?: Failed
    }
}