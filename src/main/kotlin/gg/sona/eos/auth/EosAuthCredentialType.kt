package gg.sona.eos.auth

/** Type of credentials used to log in. */
enum class EosAuthCredentialType(val value: Int) {
    Developer(0),
    NoUser(1),
    UserLogin(2),
    UserPassword(3),
    RefreshToken(4),
    ExternalAuth(5);

    companion object {
        fun fromValue(v: Int): EosAuthCredentialType = entries.firstOrNull { it.value == v } ?: NoUser
    }
}