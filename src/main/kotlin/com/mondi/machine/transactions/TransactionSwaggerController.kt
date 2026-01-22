package com.mondi.machine.transactions

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.OffsetDateTime

/**
 * The interface for Transaction Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Tag(name = "Transaction API")
interface TransactionSwaggerController {

    @Operation(summary = "Get all user transactions with filter")
    fun findAll(
        @Parameter(description = "Filter by product name") search: String?,
        @Parameter(description = "Start date for filter") purchasedAtFrom: OffsetDateTime?,
        @Parameter(description = "End date for filter") purchasedAtTo: OffsetDateTime?,
        @ParameterObject pageable: Pageable,
        httpServletRequest: HttpServletRequest
    ): Page<TransactionResponse>
}
