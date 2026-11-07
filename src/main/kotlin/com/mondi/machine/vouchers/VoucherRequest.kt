package com.mondi.machine.vouchers

import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * The request model class to create or update a [Voucher] from the backoffice.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class VoucherRequest(
    val code: String,
    val description: String? = null,
    val type: VoucherType,
    val value: BigDecimal,
    val maxDiscountAmount: BigDecimal? = null,
    val minOrderAmount: BigDecimal? = null,
    val maxUsage: Int? = null,
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime,
    val isActive: Boolean = true
)

/**
 * The request model class to check a voucher code at checkout.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class VoucherValidationRequest(
    val code: String,
    val orderSubtotal: BigDecimal
)
