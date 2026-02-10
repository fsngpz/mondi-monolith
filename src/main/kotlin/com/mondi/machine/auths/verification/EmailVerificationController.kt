package com.mondi.machine.auths.verification

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * The REST Controller for email verification endpoints.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
@RestController
@RequestMapping("/v1/auth")
class EmailVerificationController(
    private val emailVerificationTokenService: EmailVerificationTokenService,
    private val resendVerificationService: ResendVerificationService
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

    /**
     * Resend verification email to the specified email address.
     *
     * This endpoint is rate-limited to prevent abuse. Users can request
     * a new verification email if they haven't received the original one
     * or if the token has expired.
     *
     * @param request the [ResendVerificationRequest] containing the email address.
     * @return [ResendVerificationResponse] with confirmation message.
     */
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.OK)
    override fun resendVerification(@RequestBody request: ResendVerificationRequest): ResendVerificationResponse {
        return resendVerificationService.resendVerification(request.email)
    }
}
