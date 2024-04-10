package com.mondi.machine.accounts.profiles

import org.springframework.web.multipart.MultipartFile

/**
 * The model class of reqyest [Profile].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
data class ProfileRequest(
  val name: String? = null,
  val address: String? = null,
  val profilePicture: MultipartFile? = null
)