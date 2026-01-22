package com.mondi.machine.backoffices.accounts

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * The interface for Backoffice Account Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Tag(name = "Backoffice Account API")
interface BackofficeAccountSwaggerController {

    @Operation(summary = "Get all accounts with filter")
    fun findAll(
        @Parameter(description = "Filter by email or username") search: String? = null,
        @Parameter(description = "Filter by role") role: String? = null,
        @ParameterObject pageable: Pageable
    ): Page<BackofficeAccountResponse>
}
