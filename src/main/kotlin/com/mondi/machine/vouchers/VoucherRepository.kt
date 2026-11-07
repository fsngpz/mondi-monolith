package com.mondi.machine.vouchers

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

/**
 * The interface for repository [Voucher].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
interface VoucherRepository : JpaRepository<Voucher, Long> {

    /**
     * a function to find a voucher by code, ignoring case.
     */
    fun findByCodeIgnoreCase(code: String): Voucher?

    /**
     * a function to check whether a code is taken, ignoring case.
     */
    fun existsByCodeIgnoreCase(code: String): Boolean

    /**
     * a function to count one use of a voucher, only if it has uses left.
     * Done in one UPDATE so two orders cannot both take the last use.
     *
     * @param id the voucher unique identifier.
     * @return 1 if the use was counted, 0 if the voucher has no uses left.
     */
    @Modifying(flushAutomatically = true)
    @Query(
        """
    UPDATE Voucher v SET v.currentUsage = v.currentUsage + 1
    WHERE v.id = :id AND (v.maxUsage IS NULL OR v.currentUsage < v.maxUsage)
  """
    )
    fun incrementUsageIfAvailable(id: Long): Int

    /**
     * a function to give back one use of a voucher, e.g. when its order is cancelled.
     *
     * @param id the voucher unique identifier.
     * @return the number of rows updated.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Voucher v SET v.currentUsage = v.currentUsage - 1 WHERE v.id = :id AND v.currentUsage > 0")
    fun decrementUsage(id: Long): Int

    /**
     * a function to find all vouchers for the backoffice.
     *
     * @param search the search term matched against code and description, or null.
     * @param isActive the active flag to filter by, or null for all.
     * @param pageable the [Pageable].
     * @return the [Page] of [Voucher].
     */
    @Query(
        """
    FROM Voucher v
    WHERE (:isActive IS NULL OR v.isActive = :isActive)
    AND ((:search IS NULL) OR (v.code ILIKE %:#{#search}% OR v.description ILIKE %:#{#search}%))
  """
    )
    fun findAllCustom(search: String?, isActive: Boolean?, pageable: Pageable): Page<Voucher>
}
