package com.mondi.machine.auths.refresh

import com.mondi.machine.auths.users.User
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

/**
 * The entity model class for refresh tokens.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "refresh_tokens")
class RefreshToken(
    val token: String,
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    val expiresAt: ZonedDateTime
) : AuditableBaseEntity<String>() {
    var revokedAt: ZonedDateTime? = null

    /**
     * Check if the refresh token is expired.
     *
     * @return true if expired, false otherwise.
     */
    fun isExpired(): Boolean {
        return ZonedDateTime.now().isAfter(expiresAt)
    }

    /**
     * Check if the refresh token is revoked.
     *
     * @return true if revoked, false otherwise.
     */
    fun isRevoked(): Boolean {
        return revokedAt != null
    }

    /**
     * Check if the refresh token is valid (not expired and not revoked).
     *
     * @return true if valid, false otherwise.
     */
    fun isValid(): Boolean {
        return !isExpired() && !isRevoked()
    }

    /**
     * Revoke the refresh token.
     */
    fun revoke() {
        revokedAt = ZonedDateTime.now()
    }
}
