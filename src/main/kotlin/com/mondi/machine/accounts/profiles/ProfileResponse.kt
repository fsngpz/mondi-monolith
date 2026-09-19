package com.mondi.machine.accounts.profiles

import java.time.OffsetDateTime

/**
 * The model class for response [Profile].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 *
 */
data class ProfileResponse(
    val id: Long,
    val name: String?,
    val email: String,
    val emailVerifiedAt: OffsetDateTime?,
    val isEmailVerified: Boolean,
    val profilePictureUrl: String?,
    val mobile: String?,
    val membershipSince: OffsetDateTime?
)
