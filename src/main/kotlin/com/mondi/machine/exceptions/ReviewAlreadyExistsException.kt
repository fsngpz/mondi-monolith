package com.mondi.machine.exceptions

/**
 * Exception thrown when a customer reviews a product they already reviewed.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
class ReviewAlreadyExistsException(
    message: String = "You have already reviewed this product"
) : RuntimeException(message)
