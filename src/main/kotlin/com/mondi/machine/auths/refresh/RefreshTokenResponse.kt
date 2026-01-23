package com.mondi.machine.auths.refresh

/**
 * The response model class for refresh token.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
data class RefreshTokenResponse(
    val bearerToken: String,
    val refreshToken: String
)
