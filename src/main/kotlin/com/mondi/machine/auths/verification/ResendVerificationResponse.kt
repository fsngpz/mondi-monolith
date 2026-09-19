package com.mondi.machine.auths.verification

/**
 * The response model for resending email verification.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-10
 */
data class ResendVerificationResponse(
    val message: String,
    val email: String,
    val expiresInHours: Long
)
