package com.mondi.machine.auths.registration

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * The REST Controller of feature related to Registration.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@RestController
@RequestMapping("/v1/auth")
class RegistrationController(private val registrationService: RegistrationService) {

  /**
   * a function to do register the new User.
   *
   * @param request the [RegistrationRequest] payload.
   */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  fun register(@RequestBody request: RegistrationRequest) {
    // -- register the user --
    return registrationService.register(request)
  }
}