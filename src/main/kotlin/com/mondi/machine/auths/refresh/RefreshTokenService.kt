package com.mondi.machine.auths.refresh

import com.mondi.machine.auths.users.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

/**
 * The service class for refresh token operations.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(RefreshTokenService::class.java)

    /**
     * Generate a new refresh token for a user.
     *
     * @param user the [User] instance.
     * @return the generated refresh token string.
     */
    @Transactional
    fun generateRefreshToken(user: User): String {
        // -- generate unique token using UUID --
        val tokenString = UUID.randomUUID().toString()

        // -- set expiration to 7 days from now --
        val expiresAt = ZonedDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS)

        // -- create refresh token entity --
        val refreshToken = RefreshToken(
            token = tokenString,
            user = user,
            expiresAt = expiresAt
        )

        // -- save to database --
        refreshTokenRepository.save(refreshToken)

        logger.info("Generated refresh token for user: ${user.email}")

        return tokenString
    }

    /**
     * Validate and retrieve refresh token.
     *
     * @param tokenString the refresh token string.
     * @return the [RefreshToken] if valid.
     * @throws IllegalArgumentException if token is invalid, expired, or revoked.
     */
    @Transactional(readOnly = true)
    fun validateRefreshToken(tokenString: String): RefreshToken {
        // -- find token in database --
        val refreshToken = refreshTokenRepository.findByToken(tokenString)
            ?: throw IllegalArgumentException("Invalid refresh token")

        // -- check if token is expired --
        require(!refreshToken.isExpired()) {
            "Refresh token has expired"
        }

        // -- check if token is revoked --
        require(!refreshToken.isRevoked()) {
            "Refresh token has been revoked"
        }

        return refreshToken
    }

    /**
     * Revoke a specific refresh token.
     *
     * @param tokenString the refresh token string.
     */
    @Transactional
    fun revokeRefreshToken(tokenString: String) {
        val refreshToken = refreshTokenRepository.findByToken(tokenString)
        if (refreshToken != null && !refreshToken.isRevoked()) {
            refreshToken.revoke()
            refreshTokenRepository.save(refreshToken)
            logger.info("Revoked refresh token: $tokenString")
        }
    }

    /**
     * Revoke all refresh tokens for a user.
     *
     * @param user the [User] instance.
     */
    @Transactional
    fun revokeAllUserTokens(user: User) {
        val tokens = refreshTokenRepository.findAllByUser(user)
        tokens.forEach { it.revoke() }
        refreshTokenRepository.saveAll(tokens)
        logger.info("Revoked all refresh tokens for user: ${user.email}")
    }

    /**
     * Rotate refresh token - revoke old token and generate new one.
     *
     * @param oldTokenString the old refresh token string.
     * @param user the [User] instance.
     * @return the new refresh token string.
     */
    @Transactional
    fun rotateRefreshToken(oldTokenString: String, user: User): String {
        // -- revoke old token --
        revokeRefreshToken(oldTokenString)

        // -- generate new token --
        return generateRefreshToken(user)
    }

    companion object {
        const val REFRESH_TOKEN_VALIDITY_DAYS = 7L
    }
}
