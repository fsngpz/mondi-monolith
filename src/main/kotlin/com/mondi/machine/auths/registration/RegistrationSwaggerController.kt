package com.mondi.machine.auths.registration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * The interface for Registration Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Tag(name = "Registration API")
interface RegistrationSwaggerController {

    @Operation(summary = "Register new user")
    fun register(request: RegistrationRequest)
}
