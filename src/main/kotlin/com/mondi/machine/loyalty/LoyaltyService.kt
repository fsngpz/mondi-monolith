package com.mondi.machine.loyalty

import com.mondi.machine.accounts.profiles.Profile
import java.math.BigDecimal
import java.math.RoundingMode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The service class for loyalty points and tiers.
 *
 * Customers earn [POINTS_PER_AMOUNT] point per [AMOUNT_PER_POINT] IDR of an order's total once it is
 * delivered. The tier is based on the points balance.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Service
class LoyaltyService(private val repository: LoyaltyPointRepository) {

    private val log = LoggerFactory.getLogger(LoyaltyService::class.java)

    /**
     * a function to get the user's points, tier and distance to the next tier.
     *
     * @param userId the user unique identifier.
     * @return the [LoyaltyResponse].
     */
    @Transactional(readOnly = true)
    fun getBalance(userId: Long): LoyaltyResponse {
        val points = repository.sumPointsByProfileId(userId)
        val tier = LoyaltyTier.forPoints(points.coerceIn(0, Int.MAX_VALUE.toLong()).toInt())
        val next = tier.next
        return LoyaltyResponse(
            points = points,
            tier = tier,
            tierThreshold = tier.threshold,
            nextTier = next,
            nextTierThreshold = next?.threshold,
            pointsToNextTier = next?.let { (it.threshold - points).coerceAtLeast(0) },
            pointsPerAmount = POINTS_PER_AMOUNT,
            amountPerPoint = AMOUNT_PER_POINT
        )
    }

    /**
     * a function to get the user's points history, newest first.
     */
    @Transactional(readOnly = true)
    fun getHistory(userId: Long, pageable: Pageable): Page<LoyaltyHistoryResponse> {
        return repository.findAllByProfileId(userId, pageable).map {
            LoyaltyHistoryResponse(
                id = requireNotNull(it.id),
                points = it.points,
                transactionType = it.transactionType,
                orderId = it.orderId,
                description = it.description,
                createdAt = it.createdAt
            )
        }
    }

    /**
     * a function to give the points for a delivered order. Calling it again for the same order does nothing.
     *
     * @param profile the customer.
     * @param orderId the order unique identifier.
     * @param orderTotal the total the customer paid.
     * @return the points earned (0 if already earned or below one point).
     */
    @Transactional
    fun earnPoints(profile: Profile, orderId: Long, orderTotal: BigDecimal): Int {
        if (repository.existsByOrderIdAndTransactionType(orderId, LoyaltyTransactionType.EARNED)) {
            return 0
        }
        val points = pointsFor(orderTotal)
        if (points <= 0) return 0
        repository.save(
            LoyaltyPoint(
                profile = profile,
                points = points,
                transactionType = LoyaltyTransactionType.EARNED,
                orderId = orderId,
                description = "Order #$orderId delivered"
            )
        )
        log.info("Profile ${profile.id} earned $points point(s) for order $orderId")
        return points
    }

    companion object {
        const val POINTS_PER_AMOUNT = 1
        const val AMOUNT_PER_POINT = 10_000L

        /** the points for an amount: one per full 10,000 IDR. */
        fun pointsFor(amount: BigDecimal): Int =
            amount.divide(BigDecimal(AMOUNT_PER_POINT), 0, RoundingMode.DOWN).toInt() * POINTS_PER_AMOUNT
    }
}
