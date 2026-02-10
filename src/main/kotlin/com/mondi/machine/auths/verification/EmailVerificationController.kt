package com.mondi.machine.auths.verification

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * The REST Controller for email verification endpoints.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
@RestController
@RequestMapping("/v1/auth")
class EmailVerificationController(
    private val emailVerificationTokenService: EmailVerificationTokenService
) : EmailVerificationSwaggerController {

    /**
     * Verify email address using the token from the verification link.
     *
     * This endpoint can be called either with a query parameter (for link clicks)
     * or with a request body (for API calls).
     *
     * @param token the verification token (from query parameter).
     * @return [EmailVerificationResponse] with verification result.
     */
    @GetMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    override fun verifyEmail(@RequestParam token: String): EmailVerificationResponse {
        val user = emailVerificationTokenService.verifyEmail(token)

        return EmailVerificationResponse(
            message = "Email verified successfully! You can now log in to your account.",
            email = user.email,
            verified = true
        )
    }

    /**
     * Verify email address using a POST request with token in the body.
     *
     * This is an alternative endpoint for programmatic verification.
     *
     * @param request the [EmailVerificationRequest] containing the token.
     * @return [EmailVerificationResponse] with verification result.
     */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    fun verifyEmailPost(@RequestBody request: EmailVerificationRequest): EmailVerificationResponse {
        return verifyEmail(request.token)
    }
}
