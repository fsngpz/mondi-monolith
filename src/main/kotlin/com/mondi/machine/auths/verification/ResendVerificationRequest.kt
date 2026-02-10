package com.mondi.machine.auths.verification

/**
 * The request model for resending email verification.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-10
 */
data class ResendVerificationRequest(
    val email: String
)
