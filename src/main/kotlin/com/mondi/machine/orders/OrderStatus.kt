package com.mondi.machine.orders

/**
 * The enum class for the lifecycle status of an [Order].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
enum class OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /**
     * the statuses this status is allowed to move to. [DELIVERED] and [CANCELLED] are terminal.
     */
    val allowedTransitions: Set<OrderStatus>
        get() = when (this) {
            PENDING_PAYMENT -> setOf(PAYMENT_CONFIRMED, CANCELLED)
            PAYMENT_CONFIRMED -> setOf(PROCESSING, CANCELLED)
            PROCESSING -> setOf(SHIPPED, CANCELLED)
            SHIPPED -> setOf(DELIVERED)
            DELIVERED -> emptySet()
            CANCELLED -> emptySet()
        }

    /**
     * a function to check whether this status may move to [next].
     *
     * @param next the target status.
     * @return true if the transition is allowed.
     */
    fun canTransitionTo(next: OrderStatus): Boolean = next in allowedTransitions
}
