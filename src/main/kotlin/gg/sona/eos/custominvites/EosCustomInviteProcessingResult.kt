package gg.sona.eos.custominvites

/** Result of a custom invite. */
enum class EosCustomInviteProcessingResult(val value: Int) {
    Unknown(0),
    Accepted(1),
    Deferred(2),
    UnhandledError(3);

    companion object {
        internal fun fromValue(v: Int): EosCustomInviteProcessingResult =
            entries.firstOrNull { it.value == v } ?: Unknown
    }
}
