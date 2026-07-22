package gg.sona.eos.rtc

/**
 * Participant RTC status.
 */
enum class EosRtcParticipantStatus(val value: Int) {
    Joined(0),
    Left(1);

    companion object {
        fun fromValue(v: Int): EosRtcParticipantStatus = entries.firstOrNull { it.value == v } ?: Left
    }
}