package com.mondi.machine.backoffices.transactions

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType

/**
 * The interface for Backoffice Transaction Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-21
 */
@Tag(name = "Backoffice Transaction API")
interface BackofficeTransactionSwaggerController {

  @Operation(
    summary = "Create new transaction",
    requestBody = RequestBody(
      content = [Content(
        mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
        schema = Schema(implementation = BackofficeTransactionNullableRequest::class)
      )]
    )
  )
  suspend fun create(request: BackofficeTransactionNullableRequest): BackofficeTransactionResponse

  @Operation(
    summary = "Update existing transaction",
    requestBody = RequestBody(
      content = [Content(
        mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
        schema = Schema(implementation = BackofficeTransactionNullableRequest::class)
      )]
    )
  )
  suspend fun put(
    @Parameter(description = "Transaction ID") transactionId: Long,
    request: BackofficeTransactionNullableRequest
  ): BackofficeTransactionResponse
}
