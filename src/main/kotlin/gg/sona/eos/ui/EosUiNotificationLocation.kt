package gg.sona.eos.ui

/** Where the social overlay places notification toasts. */
enum class EosUiNotificationLocation(val value: Int) {
    None(0),
    TopLeft(1),
    TopRight(2),
    BottomLeft(3),
    BottomRight(4);

    companion object {
        internal fun fromValue(v: Int): EosUiNotificationLocation =
            entries.firstOrNull { it.value == v } ?: None
    }
}