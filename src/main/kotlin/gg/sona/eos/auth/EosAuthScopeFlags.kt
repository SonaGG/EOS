package gg.sona.eos.auth

/** Auth login scope flags. */
public enum class EosAuthScopeFlags(val value: Int) {
    NoFlags(0),
    BasicProfile(1),
    FriendsList(2),
    Presence(4),
    FriendsManagement(8);

    public companion object {
        public fun fromValue(v: Int): Set<EosAuthScopeFlags> = entries.filter { v and it.value != 0 }.toSet()
    }
}