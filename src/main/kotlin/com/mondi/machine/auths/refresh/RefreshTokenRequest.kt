package com.mondi.machine.auths.refresh

/**
 * The request model class for refresh token.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
data class RefreshTokenRequest(
    val refreshToken: String
)
