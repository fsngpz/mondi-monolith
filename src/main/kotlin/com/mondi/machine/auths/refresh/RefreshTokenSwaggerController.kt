package com.mondi.machine.auths.refresh

import com.mondi.machine.exceptions.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

/**
 * The Swagger documentation interface for refresh token operations.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@Tag(name = "Authentication API")
interface RefreshTokenSwaggerController {

    @Operation(summary = "Refresh access token", description = "Refresh access token using refresh token. Returns new access token and refresh token.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully refreshed tokens",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = RefreshTokenResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid or expired refresh token",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @PostMapping
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<RefreshTokenResponse>
}
