package gg.sona.eos.ecom

/** Type of catalog item. */
enum class EosEcomItemType(val value: Int) {
    Durable(0),
    Consumable(1),
    Other(2);

    companion object {
        internal fun fromValue(v: Int): EosEcomItemType = entries.firstOrNull { it.value == v } ?: Other
    }
}
