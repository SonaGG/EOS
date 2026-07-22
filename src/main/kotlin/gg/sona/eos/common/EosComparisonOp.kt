package gg.sona.eos.common

/**
 * Comparison operator used for session and lobby attribute searches.
 */
public enum class EosComparisonOp(val value: Int) {
    Equal(0),
    NotEqual(1),
    GreaterThan(2),
    GreaterThanOrEqual(3),
    LessThan(4),
    LessThanOrEqual(5),
    Nearest(6),
    AnyOf(7),
    NotAnyOf(8),
    OneOf(9),
    NotOneOf(10),
    Contains(11),
    MatchesRegex(12),
    Size(13);

    public companion object {
        internal fun fromValue(v: Int): EosComparisonOp = entries.getOrElse(v) { Equal }
    }
}
