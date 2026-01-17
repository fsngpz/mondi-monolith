package com.mondi.machine.backoffices.transactions

import com.mondi.machine.backoffices.toResponse
import com.mondi.machine.transactions.Transaction
import com.mondi.machine.transactions.TransactionRequest
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
     * @param request the [BackofficeTransactionRequest] instance.
     * @return the [BackofficeTransactionResponse] instance.
     */
    suspend fun create(request: BackofficeTransactionRequest): BackofficeTransactionResponse {
        // -- create new transaction --
        return transactionService.create(
            request.userId,
            request.productName,
            request.price,
            request.certificateFile,
            request.purchasedAt
        ).toResponse()
    }

    /**
     * a function to do an update of [Transaction].
     *
     * @param id the [Transaction] unique identifier.
     * @param request the [BackofficeTransactionRequest] instance.
     * @return the [BackofficeTransactionResponse] instance.
     */
    suspend fun update(id: Long, request: BackofficeTransactionRequest): BackofficeTransactionResponse {
        // -- setup the instance TransactionRequest --
        val transactionRequest = TransactionRequest(
            productName = request.productName,
            price = request.price,
            certificateFile = request.certificateFile,
            purchasedAt = request.purchasedAt
        )
        // -- make an update to the specified transaction --
        return transactionService.update(id, transactionRequest).toResponse()
    }
}
