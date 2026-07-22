package gg.sona.eos.metrics

enum class EosMetricsControllerType(val value: Int) {
    Unknown(0),
    MouseKeyboard(1),
    Gamepad(2),
    TouchInput(3);

    companion object {
        internal fun fromValue(v: Int): EosMetricsControllerType =
            entries.firstOrNull { it.value == v } ?: Unknown
    }
}