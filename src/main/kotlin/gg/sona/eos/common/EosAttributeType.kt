package gg.sona.eos.common

/**
 * Supported types of data that can be stored as an attribute on sessions and
 * lobbies.
 */
public enum class EosAttributeType {
    Boolean,
    Int64,
    Double,
    String;

    public companion object {
        internal fun fromValue(v: Int): EosAttributeType = when (v) {
            0 -> Boolean
            1 -> Int64
            2 -> Double
            3 -> String
            else -> Boolean
        }
    }
}
