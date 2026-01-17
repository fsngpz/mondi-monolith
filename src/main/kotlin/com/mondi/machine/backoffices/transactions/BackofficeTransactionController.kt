package com.mondi.machine.backoffices.transactions

import com.mondi.machine.backoffices.toNotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
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
     * @param request the [BackofficeTransactionNullableRequest].
     * @return the [BackofficeTransactionResponse].
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(@ModelAttribute request: BackofficeTransactionNullableRequest): BackofficeTransactionResponse {
        // -- create the data --
        return service.create(request.toNotNull())
    }

    /**
     * a PUT request to handle update the existing transaction data.
     *
     * @param transactionId the transaction unique identifier.
     * @param request the [BackofficeTransactionNullableRequest] instance.
     * @return the [BackofficeTransactionResponse] instance.
     */
    @PutMapping("/{transactionId}")
    suspend fun put(
        @PathVariable transactionId: Long,
        @ModelAttribute request: BackofficeTransactionNullableRequest
    ): BackofficeTransactionResponse {
        // -- update the data --
        return service.update(transactionId, request.toNotNull())
    }
}
