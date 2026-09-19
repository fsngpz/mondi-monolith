package com.mondi.machine.auths.verification

/**
 * The response model for email verification.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
data class EmailVerificationResponse(
    val message: String,
    val email: String,
    val verified: Boolean
)
