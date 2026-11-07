package com.mondi.machine.reviews

/**
 * The request model class to create a [Review].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class ReviewRequest(
    val productId: Long,
    val orderId: Long,
    val rating: Int,
    val comment: String? = null
)
