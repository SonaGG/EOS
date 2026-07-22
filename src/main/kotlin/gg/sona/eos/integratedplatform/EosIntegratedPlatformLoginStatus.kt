package gg.sona.eos.integratedplatform

/** Login status on an integrated platform. */
enum class EosIntegratedPlatformLoginStatus(val value: Int) {
    NotLoggedIn(0),
    LoggedIn(1);

    companion object {
        internal fun fromValue(v: Int): EosIntegratedPlatformLoginStatus =
            entries.firstOrNull { it.value == v } ?: NotLoggedIn
    }
}