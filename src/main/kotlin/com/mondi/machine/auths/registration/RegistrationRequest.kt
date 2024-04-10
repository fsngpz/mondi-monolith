package com.mondi.machine.auths.registration

/**
 * The model class of request registration.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
data class RegistrationRequest(
  val email: String?,
  val password: String?
)