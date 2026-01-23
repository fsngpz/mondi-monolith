package com.mondi.machine.auths.refresh

import com.mondi.machine.auths.users.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

/**
 * The repository interface for RefreshToken entity.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    /**
     * Find refresh token by token string.
     *
     * @param token the token string.
     * @return the [RefreshToken] if found, null otherwise.
     */
    fun findByToken(token: String): RefreshToken?

    /**
     * Find all refresh tokens for a user.
     *
     * @param user the [User] instance.
     * @return list of [RefreshToken].
     */
    fun findAllByUser(user: User): List<RefreshToken>

    /**
     * Delete all expired tokens.
     *
     * @param now the current time.
     * @return number of deleted tokens.
     */
    fun deleteByExpiresAtBefore(now: ZonedDateTime): Int

    /**
     * Delete all tokens for a user.
     *
     * @param user the [User] instance.
     * @return number of deleted tokens.
     */
    fun deleteAllByUser(user: User): Int
}
