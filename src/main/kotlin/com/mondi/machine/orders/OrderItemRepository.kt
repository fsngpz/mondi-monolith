package com.mondi.machine.orders

import org.springframework.data.jpa.repository.JpaRepository

/**
 * The interface for repository [OrderItem].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
interface OrderItemRepository : JpaRepository<OrderItem, Long>
