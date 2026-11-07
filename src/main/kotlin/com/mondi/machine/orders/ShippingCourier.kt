package com.mondi.machine.orders

/**
 * The enum class for who delivers an [Order].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
enum class ShippingCourier {
    JNE,
    JNT,
    SICEPAT,
    ANTERAJA,
    POS_INDONESIA,
    TIKI,
    NINJA_XPRESS,

    // -- Mondi's own delivery; may have no tracking number --
    IN_HOUSE;

    /**
     * whether a tracking number is required for this courier.
     */
    val requiresTrackingNumber: Boolean
        get() = this != IN_HOUSE
}
