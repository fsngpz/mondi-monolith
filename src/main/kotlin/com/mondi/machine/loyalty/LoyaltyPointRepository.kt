package com.mondi.machine.loyalty

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * The interface for repository [LoyaltyPoint].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
interface LoyaltyPointRepository : JpaRepository<LoyaltyPoint, Long> {

    /**
     * a function to get the points balance of a profile.
     */
    @Query("SELECT COALESCE(SUM(l.points), 0) FROM LoyaltyPoint l WHERE l.profile.id = :profileId")
    fun sumPointsByProfileId(profileId: Long): Long

    /**
     * a function to get the points a profile has ever earned.
     */
    @Query(
        """
    SELECT COALESCE(SUM(l.points), 0) FROM LoyaltyPoint l
    WHERE l.profile.id = :profileId AND l.transactionType = :type
  """
    )
    fun sumPointsByProfileIdAndType(profileId: Long, type: LoyaltyTransactionType): Long

    /**
     * a function to check whether an order already earned points.
     */
    fun existsByOrderIdAndTransactionType(orderId: Long, transactionType: LoyaltyTransactionType): Boolean

    /**
     * a function to get the points history of a profile.
     */
    fun findAllByProfileId(profileId: Long, pageable: Pageable): Page<LoyaltyPoint>
}
