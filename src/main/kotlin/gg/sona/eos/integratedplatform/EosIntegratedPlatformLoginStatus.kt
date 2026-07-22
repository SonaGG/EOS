package gg.sona.eos.integratedplatform

/** Login status on an integrated platform. */
public enum class EosIntegratedPlatformLoginStatus(val value: Int) {
    NotLoggedIn(0),
    LoggedIn(1);

    public companion object {
        internal fun fromValue(v: Int): EosIntegratedPlatformLoginStatus =
            entries.firstOrNull { it.value == v } ?: NotLoggedIn
    }
}