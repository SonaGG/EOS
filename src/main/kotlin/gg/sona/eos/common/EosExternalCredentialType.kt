package gg.sona.eos.common

/**
 * Identity provider used to authenticate a user.
 */
enum class EosExternalCredentialType(val value: Int) {
    Epic(0),
    SteamAppTicket(1),
    PsnIdToken(2),
    XboxXstsToken(3),
    DiscordAccessToken(4),
    GogSessionTicket(5),
    NintendoIdToken(6),
    NintendoNsaIdToken(7),
    UplayAccessToken(8),
    OpenIdAccessToken(9),
    DeviceIdAccessToken(10),
    AppleIdToken(11),
    GoogleIdToken(12),
    OculusUserIdNonce(13),
    ItchioJwt(14),
    ItchioKey(15),
    EpicIdToken(16),
    AmazonAccessToken(17),
    SteamSessionTicket(18),
    ViveportUserToken(19);

    companion object {
        internal fun fromValue(v: Int): EosExternalCredentialType = entries.firstOrNull { it.value == v } ?: Epic
    }
}