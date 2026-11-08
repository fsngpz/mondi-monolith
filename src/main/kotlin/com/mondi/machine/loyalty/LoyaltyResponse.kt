package com.mondi.machine.loyalty

import java.time.OffsetDateTime

/**
 * The response model class for a customer's loyalty status.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class LoyaltyResponse(
    val points: Long,
    val tier: LoyaltyTier,
    val tierThreshold: Int,
    // -- null at the highest tier --
    val nextTier: LoyaltyTier?,
    val nextTierThreshold: Int?,
    val pointsToNextTier: Long?,
    val pointsPerAmount: Int,
    val amountPerPoint: Long
)

/**
 * The response model class for one entry of the points history.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class LoyaltyHistoryResponse(
    val id: Long,
    val points: Int,
    val transactionType: LoyaltyTransactionType,
    val orderId: Long?,
    val description: String?,
    val createdAt: OffsetDateTime?
)
