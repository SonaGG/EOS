package gg.sona.eos.logging

/** Callback that receives log messages from the SDK. */
public fun interface EosLogMessageFunc {
    public fun onLogMessage(message: EosLogMessage)
}
