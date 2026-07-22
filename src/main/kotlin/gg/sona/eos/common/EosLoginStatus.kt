package gg.sona.eos.common

/**
 * Local user login status reported by EOS.
 */
public enum class EosLoginStatus {
    /** No user has logged in or chosen a local profile. */
    NotLoggedIn,

    /** The user is using a local profile but is not logged in. */
    UsingLocalProfile,

    /** The user has been validated by the platform-specific authentication service. */
    LoggedIn;

    public companion object {
        internal fun fromValue(v: Int): EosLoginStatus = when (v) {
            0 -> NotLoggedIn
            1 -> UsingLocalProfile
            2 -> LoggedIn
            else -> NotLoggedIn
        }
    }
}
