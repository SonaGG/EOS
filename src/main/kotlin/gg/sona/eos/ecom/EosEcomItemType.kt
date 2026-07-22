package gg.sona.eos.ecom

/** Type of catalog item. */
public enum class EosEcomItemType(val value: Int) {
    Durable(0),
    Consumable(1),
    Other(2);

    public companion object {
        internal fun fromValue(v: Int): EosEcomItemType = entries.firstOrNull { it.value == v } ?: Other
    }
}
