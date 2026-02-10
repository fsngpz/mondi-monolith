package com.mondi.machine.auths.verification

import com.mondi.machine.auths.users.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

/**
 * The repository interface for [EmailVerificationToken].
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
@Repository
interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, Long> {

    /**
     * Find a verification token by its token string.
     *
     * @param token the token string.
     * @return Optional containing the token if found.
     */
    fun findByToken(token: String): Optional<EmailVerificationToken>

    /**
     * Find all valid (not expired and not verified) tokens for a user.
     *
     * @param user the [User] instance.
     * @param now the current timestamp.
     * @return list of valid tokens.
     */
    @Query(
        """
        SELECT t FROM EmailVerificationToken t
        WHERE t.user = :user
        AND t.verifiedAt IS NULL
        AND t.expiresAt > :now
    """
    )
    fun findValidTokensByUser(
        @Param("user") user: User,
        @Param("now") now: OffsetDateTime
    ): List<EmailVerificationToken>

    /**
     * Delete all tokens for a specific user.
     *
     * @param user the [User] instance.
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user = :user")
    fun deleteByUser(@Param("user") user: User)

    /**
     * Delete all expired tokens.
     *
     * @param now the current timestamp.
     * @return number of deleted tokens.
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    fun deleteExpiredTokens(@Param("now") now: OffsetDateTime): Int

    /**
     * Check if a user has any valid tokens.
     *
     * @param user the [User] instance.
     * @param now the current timestamp.
     * @return true if valid tokens exist.
     */
    @Query(
        """
        SELECT COUNT(t) > 0 FROM EmailVerificationToken t
        WHERE t.user = :user
        AND t.verifiedAt IS NULL
        AND t.expiresAt > :now
    """
    )
    fun existsValidTokenForUser(
        @Param("user") user: User,
        @Param("now") now: OffsetDateTime
    ): Boolean
}
