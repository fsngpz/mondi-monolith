package com.mondi.machine.auths.verification

/**
 * The request model for email verification.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
data class EmailVerificationRequest(
    val token: String
)
