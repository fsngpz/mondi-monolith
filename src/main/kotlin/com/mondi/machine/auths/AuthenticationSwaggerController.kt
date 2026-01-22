package com.mondi.machine.auths

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * The interface for Authentication Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Tag(name = "Authentication API")
interface AuthenticationSwaggerController {

    @Operation(summary = "Login to the system")
    fun login(request: AuthenticationRequest): AuthenticationResponse
}
