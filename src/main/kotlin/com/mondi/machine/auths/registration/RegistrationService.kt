package com.mondi.machine.auths.registration

import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRegistrationService
import org.springframework.stereotype.Service

/**
 * The service class to handle feature registration.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Service
class RegistrationService(private val userRegistrationService: UserRegistrationService) {

  /**
   * a function to handle request register new [User].
   *
   * @param request the [RegistrationRequest].
   */
  fun register(request: RegistrationRequest) {
    // -- validate field 'email' --
    requireNotNull(request.email) {
      "field 'email' cannot be null"
    }
    // -- validate field 'password' --
    requireNotNull(request.password) {
      "field 'password' cannot be null"
    }
    // -- create the new user --
    userRegistrationService.create(email = request.email, password = request.password)
  }
}