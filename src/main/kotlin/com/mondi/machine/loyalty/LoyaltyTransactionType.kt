package com.mondi.machine.loyalty

/**
 * The enum class for why a [LoyaltyPoint] entry changed the balance.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
enum class LoyaltyTransactionType {
    EARNED,
    REDEEMED,
    EXPIRED,
    ADJUSTMENT
}
