package com.mondi.machine.auths

/**
 * The model class for request authentication.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
data class AuthenticationRequest(
  val email: String? = null,
  val password: String? = null
)