package gg.sona.eos.logging

/** Callback that receives log messages from the SDK. */
fun interface EosLogMessageFunc {
    fun onLogMessage(message: EosLogMessage)
}
