package com.mondi.machine.auths

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The controller class for feature related to Authentication.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@RestController
@RequestMapping("/v1/auth")
class AuthenticationController(private val service: AuthenticationService) : AuthenticationSwaggerController {

  /**
   * a POST request to handle login.
   *
   * @param request the [AuthenticationRequest] payload.
   * @return the [AuthenticationResponse] instance.
   */
  @PostMapping("/login")
  override fun login(@RequestBody request: AuthenticationRequest): AuthenticationResponse {
    // -- validate field email --
    requireNotNull(request.email) {
      "field 'email' cannot be null"
    }
    // -- validate field password --
    requireNotNull(request.password) {
      "field 'password' cannot be null"
    }
    // -- execute the service --
    return service.login(request.email, request.password)
  }
}