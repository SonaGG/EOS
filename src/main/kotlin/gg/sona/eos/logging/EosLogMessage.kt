package gg.sona.eos.logging

/** A log message emitted by the SDK. */
class EosLogMessage(
    val category: String,
    val message: String,
    val level: EosLogLevel,
)
