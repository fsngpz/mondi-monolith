package com.mondi.machine.orders

/**
 * The request model class to place an [Order] from the cart.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class CreateOrderRequest(
    val addressId: Long,
    val paymentMethod: PaymentMethod,
    val voucherCode: String? = null,
    val notes: String? = null,
    val items: List<CreateOrderItemRequest>
)

/**
 * The request model class for one cart line of [CreateOrderRequest].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class CreateOrderItemRequest(
    val productId: Long,
    val quantity: Int
)
