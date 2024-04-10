package com.mondi.machine.auths

import com.mondi.machine.auths.jwt.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * The service class for authentication feature.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Service
class AuthenticationService(
  private val jwtService: JwtService,
  private val authenticationManager: AuthenticationManager
) {

  /**
   * a function to handle request login.
   *
   * @param email the email of user.
   * @param password the password of user.
   * @return the JWT token in [AuthenticationResponse].
   */
  fun login(email: String, password: String): AuthenticationResponse {
    // -- setup the instance for email and password --
    val request = UsernamePasswordAuthenticationToken(email, password)
    // -- check is authenticated --
    val authentication = authenticationManager.authenticate(request)
    // -- check is user authenticated or not. if not, an exception will be thrown --
    require(authentication.isAuthenticated) {
      throw UsernameNotFoundException("invalid username and password!")
    }
    // -- generate the token --
    val bearerToken = jwtService.generateToken(email)
    // -- return the token --
    return AuthenticationResponse(bearerToken)
  }
}