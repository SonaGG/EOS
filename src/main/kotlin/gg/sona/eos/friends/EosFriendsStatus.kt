package gg.sona.eos.friends

/** Status between two friends. */
public enum class EosFriendsStatus(val value: Int) {
    NotFriends(0),
    InviteSent(1),
    InviteReceived(2),
    Friends(3);

    public companion object {
        public fun fromValue(v: Int): EosFriendsStatus = entries.firstOrNull { it.value == v } ?: NotFriends
    }
}