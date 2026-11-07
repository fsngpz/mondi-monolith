package com.mondi.machine.orders

/**
 * The enum class for who delivers an [Order].
 *
 * @param displayName the name shown to customers.
 * @param websiteUrl the courier's website, where customers can look up a tracking number.
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
enum class ShippingCourier(val displayName: String, val websiteUrl: String?) {
    JNE("JNE", "https://www.jne.co.id"),
    JNT("J&T Express", "https://jet.co.id"),
    SICEPAT("SiCepat", "https://www.sicepat.com"),
    ANTERAJA("AnterAja", "https://anteraja.id"),
    POS_INDONESIA("Pos Indonesia", "https://www.posindonesia.co.id"),
    TIKI("TIKI", "https://www.tiki.id"),
    NINJA_XPRESS("Ninja Xpress", "https://www.ninjaxpress.co"),

    // -- Mondi's own delivery; may have no tracking number --
    IN_HOUSE("Mondi delivery", null);

    /**
     * whether a tracking number is required for this courier.
     */
    val requiresTrackingNumber: Boolean
        get() = this != IN_HOUSE
}
