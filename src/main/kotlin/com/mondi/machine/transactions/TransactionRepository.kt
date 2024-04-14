package com.mondi.machine.transactions

import java.time.OffsetDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * The interface for repository [Transaction].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-15
 */
interface TransactionRepository : JpaRepository<Transaction, Long> {

  /**
   * a function to find all [Transaction] with custom logic.
   *
   * @param id the user / profile unique identifier.
   * @param search the parameter to filter data by product name.
   * @param purchasedAtFrom the starts date to filter data by purchased at.
   * @param purchasedAtTo the ends date to filter data by purchased at.
   * @param pageable the [Pageable].
   * @return the [Page] of [Transaction].
   */
  @Query(
    """
    FROM Transaction t
    WHERE t.profile.id = :id
    AND ((:search IS NULL ) OR (t.productName ILIKE %:#{#search}%))
    AND (t.purchasedAt >= :purchasedAtFrom)
    AND (t.purchasedAt <= :purchasedAtTo)
  """
  )
  fun findAllCustom(
    id: Long,
    search: String?,
    purchasedAtFrom: OffsetDateTime,
    purchasedAtTo: OffsetDateTime,
    pageable: Pageable
  ): Page<Transaction>
}