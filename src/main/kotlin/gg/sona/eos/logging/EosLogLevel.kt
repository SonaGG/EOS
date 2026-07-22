package gg.sona.eos.logging

/** Verbosity of log messages. Higher values mean more chatty logs. */
enum class EosLogLevel(val value: Int) {
    Off(0),
    Fatal(100),
    Error(200),
    Warning(300),
    Info(400),
    Verbose(500),
    VeryVerbose(600);

    companion object {
        fun fromValue(v: Int): EosLogLevel = entries.firstOrNull { it.value == v } ?: Off
    }
}