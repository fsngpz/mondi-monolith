package com.mondi.machine.loyalty

/**
 * The enum class for loyalty tiers, by lifetime points. Ordered from lowest to highest.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
enum class LoyaltyTier(val threshold: Int) {
    BRONZE(0),
    SILVER(1_000),
    GOLD(3_000),
    DIAMOND(10_000);

    /** the next tier up, or null at the top. */
    val next: LoyaltyTier?
        get() = entries.getOrNull(ordinal + 1)

    companion object {
        /** the tier for a points balance. */
        fun forPoints(points: Int): LoyaltyTier = entries.last { points >= it.threshold }
    }
}
