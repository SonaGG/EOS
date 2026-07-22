package gg.sona.eos.ecom

/** Ownership status. */
public enum class EosOwnershipStatus(val value: Int) {
    NotOwned(0),
    Owned(1);

    public companion object {
        internal fun fromValue(v: Int): EosOwnershipStatus = entries.firstOrNull { it.value == v } ?: NotOwned
    }
}