package gg.sona.eos.ecom

/** Orientation preference for the mobile checkout page. */
public enum class EosCheckoutOrientation(val value: Int) {
    Default(0),
    Portrait(1),
    Landscape(2);

    public companion object {
        internal fun fromValue(v: Int): EosCheckoutOrientation = entries.firstOrNull { it.value == v } ?: Default
    }
}