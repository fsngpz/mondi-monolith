package com.mondi.machine.orders

/**
 * The event published when an order is marked as shipped. Carries everything the email needs,
 * so the listener does not load entities after the transaction.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class OrderShippedEvent(
    val orderId: Long,
    val email: String,
    val customerName: String?,
    val courier: ShippingCourier,
    val trackingNumber: String?,
    val items: List<Item>
) {
    data class Item(val name: String, val quantity: Int)

    companion object {
        /**
         * a function to build the event from an order, or null when the order has no customer
         * (deleted account) or no shipment.
         */
        fun from(order: Order): OrderShippedEvent? {
            val profile = order.profile ?: return null
            val courier = order.shippingCourier ?: return null
            return OrderShippedEvent(
                orderId = requireNotNull(order.id),
                email = profile.user.email,
                customerName = profile.name,
                courier = courier,
                trackingNumber = order.trackingNumber,
                items = order.items.map { Item(it.productName, it.quantity) }
            )
        }
    }
}
