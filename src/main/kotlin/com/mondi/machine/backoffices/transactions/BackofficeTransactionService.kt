package com.mondi.machine.backoffices.transactions

import com.mondi.machine.backoffices.toResponse
import com.mondi.machine.transactions.TransactionService
import org.springframework.stereotype.Service

/**
 * The service class for backoffice related to transaction.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-15
 */
@Service
class BackofficeTransactionService(private val transactionService: TransactionService) {

  /**
   * a function to handle request create new transaction.
   *
   * @param request the [BackofficeTransactionRequest].
   * @return the [BackofficeTransactionResponse].
   */
  fun create(request: BackofficeTransactionRequest): BackofficeTransactionResponse {
    // -- validate field userId --
    requireNotNull(request.userId) {
      "the field 'userId' cannot be null"
    }
    // -- validate field productName --
    requireNotNull(request.productName) {
      "the field 'productName' cannot be null"
    }
    // -- validate field price --
    requireNotNull(request.price) {
      "the field 'price' cannot be null"

    }
    // -- validate field certificateFile --
    requireNotNull(request.certificateFile) {
      "the field 'certificateFile' cannot be null"

    }
    // -- validate field purchasedAt --
    requireNotNull(request.purchasedAt) {
      "the field 'purchasedAt' cannot be null"

    }
    // -- create new transaction --
    return transactionService.create(
      request.userId,
      request.productName,
      request.price,
      request.certificateFile,
      request.purchasedAt
    ).toResponse()
  }
}