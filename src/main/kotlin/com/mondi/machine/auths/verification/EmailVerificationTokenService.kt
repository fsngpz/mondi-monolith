package com.mondi.machine.auths.verification

import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRepository
import com.mondi.machine.exceptions.EmailVerificationTokenNotFoundException
import com.mondi.machine.exceptions.InvalidEmailVerificationTokenException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

/**
 * The service class for managing email verification tokens.
 *
 * This service handles creation, validation, and expiration of email verification tokens.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
@Service
class EmailVerificationTokenService(
    private val tokenRepository: EmailVerificationTokenRepository,
    private val userRepository: UserRepository,
    @Value("\${app.email-verification.token-expiry-hours:24}") private val tokenExpiryHours: Long
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Generate a new verification token for a user.
     *
     * If the user already has valid tokens, they will be invalidated before creating a new one.
     *
     * @param user the [User] for whom to generate the token.
     * @return the generated [EmailVerificationToken].
     */
    @Transactional
    fun generateToken(user: User): EmailVerificationToken {
        logger.info("Generating verification token for user: ${user.email}")

        // -- invalidate any existing valid tokens for this user --
        invalidateUserTokens(user)

        // -- generate unique token --
        val token = UUID.randomUUID().toString()
        val expiresAt = OffsetDateTime.now().plusHours(tokenExpiryHours)

        // -- create and save token --
        val verificationToken = EmailVerificationToken(
            user = user,
            token = token,
            expiresAt = expiresAt
        )

        tokenRepository.save(verificationToken)
        logger.info("Verification token generated successfully for user: ${user.email}, expires at: $expiresAt")

        return verificationToken
    }

    /**
     * Verify an email using a token string.
     *
     * This will mark the token as used and update the user's email_verified_at timestamp.
     *
     * @param tokenString the token string to verify.
     * @return the verified [User].
     * @throws EmailVerificationTokenNotFoundException if token is not found.
     * @throws InvalidEmailVerificationTokenException if token is expired or already used.
     */
    @Transactional
    fun verifyEmail(tokenString: String): User {
        logger.info("Attempting to verify email with token: $tokenString")

        // -- find token --
        val token = tokenRepository.findByToken(tokenString)
            .orElseThrow { EmailVerificationTokenNotFoundException("Verification token not found") }

        // -- validate token --
        if (token.isExpired) {
            logger.warn("Token is expired: $tokenString")
            throw InvalidEmailVerificationTokenException("Verification token has expired. Please request a new one.")
        }

        if (token.isVerified) {
            logger.warn("Token already used: $tokenString")
            throw InvalidEmailVerificationTokenException("This verification link has already been used.")
        }

        // -- mark token as verified --
        token.verifiedAt = OffsetDateTime.now()
        tokenRepository.save(token)

        // -- update user's email_verified_at --
        val user = token.user
        if (user.emailVerifiedAt == null) {
            user.emailVerifiedAt = OffsetDateTime.now()
            userRepository.save(user)
            logger.info("Email verified successfully for user: ${user.email}")
        } else {
            logger.info("Email already verified for user: ${user.email}, but token was valid")
        }

        return user
    }

    /**
     * Invalidate all valid tokens for a user.
     *
     * This is useful when generating a new token or when the user requests a password reset.
     *
     * @param user the [User] whose tokens should be invalidated.
     */
    @Transactional
    fun invalidateUserTokens(user: User) {
        val validTokens = tokenRepository.findValidTokensByUser(user, OffsetDateTime.now())
        if (validTokens.isNotEmpty()) {
            logger.info("Invalidating ${validTokens.size} valid token(s) for user: ${user.email}")
            validTokens.forEach { it.verifiedAt = OffsetDateTime.now() }
            tokenRepository.saveAll(validTokens)
        }
    }

    /**
     * Delete all tokens for a specific user.
     *
     * @param user the [User] whose tokens should be deleted.
     */
    @Transactional
    fun deleteUserTokens(user: User) {
        logger.info("Deleting all tokens for user: ${user.email}")
        tokenRepository.deleteByUser(user)
    }

    /**
     * Clean up expired tokens from the database.
     *
     * This should be called periodically (e.g., via a scheduled task) to remove old tokens.
     *
     * @return number of tokens deleted.
     */
    @Transactional
    fun cleanupExpiredTokens(): Int {
        logger.info("Starting cleanup of expired tokens")
        val deletedCount = tokenRepository.deleteExpiredTokens(OffsetDateTime.now())
        logger.info("Deleted $deletedCount expired token(s)")
        return deletedCount
    }

    /**
     * Check if a user has any valid verification tokens.
     *
     * @param user the [User] to check.
     * @return true if the user has valid tokens.
     */
    fun hasValidToken(user: User): Boolean {
        return tokenRepository.existsValidTokenForUser(user, OffsetDateTime.now())
    }

    /**
     * Get the token expiry duration in hours.
     *
     * @return the token expiry duration.
     */
    fun getTokenExpiryHours(): Long {
        return tokenExpiryHours
    }
}
