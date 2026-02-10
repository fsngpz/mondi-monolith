package com.mondi.machine.auths.verification

import com.mondi.machine.auths.users.User
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime

/**
 * The entity model class for email verification tokens.
 *
 * This entity stores tokens used to verify user email addresses.
 * Each token has an expiration time for security purposes.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "email_verification_tokens")
class EmailVerificationToken(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, unique = true, length = 255)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,

    @Column(name = "verified_at")
    var verifiedAt: OffsetDateTime? = null
) : AuditableBaseEntity<String>() {

    /**
     * Check if the token has expired.
     *
     * @return true if the token is expired, false otherwise.
     */
    val isExpired: Boolean
        get() = OffsetDateTime.now().isAfter(expiresAt)

    /**
     * Check if the token has been used for verification.
     *
     * @return true if the token has been verified, false otherwise.
     */
    val isVerified: Boolean
        get() = verifiedAt != null

    /**
     * Check if the token is valid (not expired and not used).
     *
     * @return true if the token is valid, false otherwise.
     */
    val isValid: Boolean
        get() = !isExpired && !isVerified
}
