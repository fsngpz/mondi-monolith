package com.mondi.machine.accounts.profiles

import java.time.OffsetDateTime

/**
 * The model class of reqyest [Profile].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
data class ProfileRequest(
    val name: String? = null,
    val profilePictureKey: String? = null,
    val mobile: String? = null,
    val membershipSince: OffsetDateTime? = null
)
