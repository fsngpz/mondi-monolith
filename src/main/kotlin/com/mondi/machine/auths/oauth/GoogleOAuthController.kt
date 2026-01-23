package com.mondi.machine.auths.oauth

import com.mondi.machine.auths.AuthenticationResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The controller class for Google OAuth authentication.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@RestController
@RequestMapping("/v1/auth/oauth")
class GoogleOAuthController(private val service: GoogleOAuthService) : GoogleOAuthSwaggerController {

    /**
     * a POST request to authenticate with Google OAuth.
     *
     * @param request the [GoogleOAuthRequest] payload.
     * @return the [AuthenticationResponse] instance.
     */
    @PostMapping("/google")
    override fun authenticateWithGoogle(@RequestBody request: GoogleOAuthRequest): AuthenticationResponse {
        // -- validate field idToken --
        requireNotNull(request.idToken) {
            "field 'idToken' cannot be null"
        }
        // -- execute the service --
        return service.authenticate(request.idToken)
    }
}
