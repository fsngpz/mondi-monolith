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
  val address: String?,
  val profilePictureUrl: String?,
  val mobile: String?,
  val membershipSince: OffsetDateTime?
)