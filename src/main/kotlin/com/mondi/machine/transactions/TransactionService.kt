package com.mondi.machine.transactions

import java.time.OffsetDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * The service class for [Transaction].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
@Service
class TransactionService(private val repository: TransactionRepository) {

  /**
   * a function to find all [Transaction] of specified user.
   *
   * @param id the user / profile unique identifier.
   * @param search the parameter to filter data by product name.
   * @param purchasedAtFrom the starts date to filter data by purchased at.
   * @param purchasedAtTo the ends date to filter data by purchased at.
   * @param pageable the [Pageable].
   * @return the [Page] of [TransactionResponse].
   */
  fun findAll(
    id: Long,
    search: String?,
    purchasedAtFrom: OffsetDateTime,
    purchasedAtTo: OffsetDateTime,
    pageable: Pageable
  ): Page<TransactionResponse> {
    // -- find the data --
    return repository.findAllCustom(id, search, purchasedAtFrom, purchasedAtTo, pageable)
      .map { it.toResponse() }
  }
}