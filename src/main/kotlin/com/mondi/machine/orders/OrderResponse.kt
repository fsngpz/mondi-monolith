package com.mondi.machine.orders

import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * The response model class for [Order].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class OrderResponse(
    val id: Long,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod,
    val shippingAddress: OrderShippingAddressResponse,
    val subtotal: BigDecimal,
    val discountAmount: BigDecimal,
    val totalPrice: BigDecimal,
    val voucherCode: String?,
    val paymentProofUrl: String?,
    val notes: String?,
    val shipment: OrderShipmentResponse?,
    val items: List<OrderItemResponse>,
    val customer: OrderCustomerResponse?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?
)

/**
 * The response model class for the shipping address snapshot of an [Order].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class OrderShippingAddressResponse(
    val recipientName: String,
    val phone: String,
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)

/**
 * The response model class for the shipment of an [Order]. Null on the order until a courier is set.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class OrderShipmentResponse(
    val courier: ShippingCourier,
    val trackingNumber: String?,
    val shippedAt: OffsetDateTime?
)

/**
 * The response model class for [OrderItem].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class OrderItemResponse(
    val id: Long,
    val productId: Long?,
    val productName: String,
    val productSku: String,
    val price: BigDecimal,
    val quantity: Int,
    val certificateUrl: String?,
    val mediaUrl: String?
)

/**
 * The response model class for the customer who placed an [Order].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class OrderCustomerResponse(
    val id: Long,
    val email: String,
    val name: String?,
    val mobile: String?
)
