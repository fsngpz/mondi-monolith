package com.mondi.machine.exceptions

/**
 * Exception thrown when an order asks for more units of a product than are in stock.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
class InsufficientStockException(
    message: String = "Not enough stock for this product"
) : RuntimeException(message)
