package gg.sona.eos.lobby

/** Status of a lobby member. */
enum class EosLobbyMemberStatus(val value: Int) {
    Joined(0),
    Left(1),
    Disconnected(2),
    Kicked(3),
    Promoted(4);

    companion object {
        internal fun fromValue(v: Int): EosLobbyMemberStatus =
            entries.firstOrNull { it.value == v } ?: Left
    }
}