package gg.sona.eos.common

/**
 * Status of desktop crossplay functionality on Windows.
 */
enum class EosDesktopCrossplayStatus(val value: Int) {
    /** Desktop crossplay is ready to use. */
    Ok(0),

    /** The application was not launched through the Bootstrapper. */
    ApplicationNotBootstrapped(1),

    /** The redistributable service is not installed. */
    ServiceNotInstalled(2),

    /** The service failed to start. */
    ServiceStartFailed(3),

    /** The service is no longer running. */
    ServiceNotRunning(4),

    /** The overlay is disabled. */
    OverlayDisabled(5),

    /** The overlay is not installed. */
    OverlayNotInstalled(6),

    /** The overlay failed trust check. */
    OverlayTrustCheckFailed(7),

    /** The overlay failed to load. */
    OverlayLoadFailed(8);

    companion object {
        fun fromValue(v: Int): EosDesktopCrossplayStatus =
            entries.firstOrNull { it.value == v } ?: Ok
    }
}
