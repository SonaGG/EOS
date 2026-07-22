package gg.sona.eos.rtc

/**
 * Participant RTC status.
 */
public enum class EosRtcParticipantStatus(val value: Int) {
    Joined(0),
    Left(1);

    public companion object {
        public fun fromValue(v: Int): EosRtcParticipantStatus = entries.firstOrNull { it.value == v } ?: Left
    }
}