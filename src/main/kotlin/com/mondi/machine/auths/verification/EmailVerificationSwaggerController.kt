package com.mondi.machine.auths.verification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * The swagger interface for email verification endpoints.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
@Tag(name = "Email Verification", description = "Endpoints for email verification")
interface EmailVerificationSwaggerController {

    @Operation(
        summary = "Verify email address",
        description = "Verifies a user's email address using the verification token sent to their email"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Email verified successfully",
                content = [Content(schema = Schema(implementation = EmailVerificationResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid or expired token"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Token not found"
            )
        ]
    )
    fun verifyEmail(token: String): EmailVerificationResponse
}
