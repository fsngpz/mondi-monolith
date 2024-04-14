package com.mondi.machine.backoffices.transactions

import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for backoffice related to transaction.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-15
 */
@RestController
@RequestMapping("/v1/backoffice/transactions")
class BackofficeTransactionController(private val service: BackofficeTransactionService) {

  /**
   * a POST request to handle create new transaction data.
   *
   * @param request the [BackofficeTransactionRequest].
   * @return the [BackofficeTransactionResponse].
   */
  @PostMapping
  fun create(@ModelAttribute request: BackofficeTransactionRequest): BackofficeTransactionResponse {
    // -- create the data --
    return service.create(request)
  }
}