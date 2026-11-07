package com.mondi.machine.exceptions

/**
 * Exception thrown when an order status change is not one of the allowed transitions.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
class InvalidOrderStatusTransitionException(
    message: String = "This order status change is not allowed"
) : RuntimeException(message)
