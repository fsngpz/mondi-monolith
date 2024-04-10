package com.mondi.machine.auths.jwt

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * The entry point class for JWT Auth.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Component
class JwtAuthEntryPoint : AuthenticationEntryPoint {
  override fun commence(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authException: AuthenticationException
  ) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.message)
  }
}