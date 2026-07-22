package gg.sona.eos.common

/**
 * External account provider identifiers supported by EOS.
 */
public enum class EosExternalAccountType {
    Epic,
    Steam,
    Psn,
    XboxLive,
    Discord,
    Gog,
    Nintendo,
    Uplay,
    OpenId,
    Apple,
    Google,
    Oculus,
    Itchio,
    Amazon,
    Viveport;

    public companion object {
        internal fun fromValue(v: Int): EosExternalAccountType = entries.getOrElse(v) { Epic }
    }
}