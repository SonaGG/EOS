package gg.sona.eos.rtc

/** RTC audio status of a participant or a room. */
enum class EosRtcAudioStatus(val value: Int) {
    Unsupported(0),
    Enabled(1),
    Disabled(2),
    AdminDisabled(3),
    NotListeningDisabled(4);

    companion object {
        fun fromValue(v: Int): EosRtcAudioStatus = entries.firstOrNull { it.value == v } ?: Unsupported
    }
}