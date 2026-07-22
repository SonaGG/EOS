package gg.sona.eos.logging

/** A log message emitted by the SDK. */
public class EosLogMessage(
    public val category: String,
    public val message: String,
    public val level: EosLogLevel,
)
