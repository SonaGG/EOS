package gg.sona.eos.common

/**
 * The application state that the platform should treat the process as being in.
 */
enum class EosApplicationStatus(val value: Int) {
    /** Xbox only: the application has entered constrained mode. */
    BackgroundConstrained(0),

    /** Xbox only: the application has returned from constrained mode. */
    BackgroundUnconstrained(1),

    /** The application has been suspended. */
    BackgroundSuspended(2),

    /** The application is in the foreground. */
    Foreground(3);

    companion object {
        fun fromValue(v: Int): EosApplicationStatus = entries.firstOrNull { it.value == v } ?: Foreground
    }
}
