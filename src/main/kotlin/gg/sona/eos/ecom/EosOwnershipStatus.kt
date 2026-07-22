package gg.sona.eos.ecom

/** Ownership status. */
enum class EosOwnershipStatus(val value: Int) {
    NotOwned(0),
    Owned(1);

    companion object {
        internal fun fromValue(v: Int): EosOwnershipStatus = entries.firstOrNull { it.value == v } ?: NotOwned
    }
}