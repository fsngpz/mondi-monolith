package com.mondi.machine.backoffices.orders

import com.mondi.machine.orders.OrderStatus
import com.mondi.machine.orders.ShippingCourier

/**
 * The request model class to change the status of an order from the backoffice.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class BackofficeOrderStatusRequest(
    val status: OrderStatus,
    // -- optional: set the shipment in the same request, e.g. when marking the order SHIPPED --
    val courier: ShippingCourier? = null,
    val trackingNumber: String? = null
)

/**
 * The request model class to set or correct the shipment of an order from the backoffice.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class BackofficeOrderShipmentRequest(
    val courier: ShippingCourier,
    val trackingNumber: String? = null
)
