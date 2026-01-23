package com.mondi.machine.auths.oauth

import com.mondi.machine.auths.AuthenticationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * The interface for Google OAuth Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Tag(name = "Google OAuth API")
interface GoogleOAuthSwaggerController {

    @Operation(summary = "Authenticate with Google OAuth")
    fun authenticateWithGoogle(request: GoogleOAuthRequest): AuthenticationResponse
}
