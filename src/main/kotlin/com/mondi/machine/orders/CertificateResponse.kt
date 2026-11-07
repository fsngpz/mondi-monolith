package com.mondi.machine.orders

import java.time.OffsetDateTime

/**
 * The response model class for a customer's certificate.
 *
 * Certificates come from order items (checkout orders) or from legacy transactions created by an
 * admin before checkout existed. Legacy certificates have no order or order item id.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class CertificateResponse(
    val source: CertificateSource,
    val orderItemId: Long?,
    val orderId: Long?,
    val productName: String,
    val productSku: String?,
    val certificateUrl: String,
    val mediaUrl: String?,
    val purchasedAt: OffsetDateTime?
)

/**
 * Where a [CertificateResponse] comes from.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
enum class CertificateSource {
    ORDER,
    LEGACY_TRANSACTION
}

/**
 * The response model class for a certificate in the backoffice list.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class BackofficeCertificateResponse(
    val orderItemId: Long,
    val orderId: Long,
    val orderStatus: OrderStatus,
    val productName: String,
    val productSku: String,
    val certificateUrl: String,
    val customer: OrderCustomerResponse?,
    val purchasedAt: OffsetDateTime?,
    val uploadedAt: OffsetDateTime?
)
