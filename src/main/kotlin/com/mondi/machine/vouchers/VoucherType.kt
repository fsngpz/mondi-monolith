package com.mondi.machine.vouchers

/**
 * The enum class for how a [Voucher] discounts an order.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
enum class VoucherType {
    // -- value is a percentage of the subtotal (1-100), optionally capped by maxDiscountAmount --
    PERCENTAGE,

    // -- value is an amount in IDR, never more than the subtotal --
    FIXED
}
