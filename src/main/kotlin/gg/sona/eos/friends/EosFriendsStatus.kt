package gg.sona.eos.friends

/** Status between two friends. */
enum class EosFriendsStatus(val value: Int) {
    NotFriends(0),
    InviteSent(1),
    InviteReceived(2),
    Friends(3);

    companion object {
        fun fromValue(v: Int): EosFriendsStatus = entries.firstOrNull { it.value == v } ?: NotFriends
    }
}