package com.mondi.machine.backoffices.orders

import com.mondi.machine.orders.OrderStatus

/**
 * The request model class to change the status of an order from the backoffice.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
data class BackofficeOrderStatusRequest(
    val status: OrderStatus
)
