package com.mondi.machine.vouchers

import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * The backoffice response model class for a [Voucher].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class VoucherResponse(
    val id: Long,
    val code: String,
    val description: String?,
    val type: VoucherType,
    val value: BigDecimal,
    val maxDiscountAmount: BigDecimal?,
    val minOrderAmount: BigDecimal,
    val maxUsage: Int?,
    val currentUsage: Int,
    val startDate: OffsetDateTime,
    val endDate: OffsetDateTime,
    val isActive: Boolean,
    val createdAt: OffsetDateTime?
)

/**
 * The response model class of a voucher check at checkout.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class VoucherValidationResponse(
    val valid: Boolean,
    val code: String,
    val discountAmount: BigDecimal,
    val message: String
)

/**
 * an extension function to map the [Voucher] to [VoucherResponse].
 *
 * @return the [VoucherResponse] instance.
 */
fun Voucher.toResponse(): VoucherResponse {
    return VoucherResponse(
        id = requireNotNull(this.id) { "field id is null" },
        code = this.code,
        description = this.description,
        type = this.type,
        value = this.value,
        maxDiscountAmount = this.maxDiscountAmount,
        minOrderAmount = this.minOrderAmount,
        maxUsage = this.maxUsage,
        currentUsage = this.currentUsage,
        startDate = this.startDate,
        endDate = this.endDate,
        isActive = this.isActive,
        createdAt = this.createdAt
    )
}
