package com.mondi.machine.auths

/**
 * The model class for response authentication.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
data class AuthenticationResponse(
  val bearerToken: String
)