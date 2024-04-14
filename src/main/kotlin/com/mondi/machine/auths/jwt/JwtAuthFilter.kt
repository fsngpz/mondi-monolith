package com.mondi.machine.auths.jwt

import com.mondi.machine.configs.CustomHeaderHttpServletRequest
import com.mondi.machine.configs.CustomUserDetailService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * The authentication filter class for Json Web Token.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Component
class JwtAuthFilter(
  private val jwtService: JwtService,
  private val customUserDetailService: CustomUserDetailService
) : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val customHeader = HashMap<String, String>()
    val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
    var token: String? = null
    var email: String? = null
    var id: String? = null
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      token = authHeader.substring("Bearer ".length)
      email = jwtService.extractEmail(token)
      id = jwtService.extractId(token)
    }

    if (id != null && email != null && SecurityContextHolder.getContext().authentication == null) {
      val userDetails: UserDetails = customUserDetailService.loadUserByUsername(email)
      if (jwtService.validateToken(token, userDetails)) {
        // -- setup instance AuthenticationToken --
        val authToken = UsernamePasswordAuthenticationToken(
          userDetails,
          null,
          userDetails.authorities
        )
        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authToken
        // -- get the CustomUserDetails --
        // val customUserDetails = customUserDetailService.getCustomUserDetails(email)
      }
      // -- set the custom header --
      customHeader.apply {
        this["ID"] = id
      }
    }
    // -- do customize the HttpServletRequest --
    val newRequest = CustomHeaderHttpServletRequest(request, customHeader)
    filterChain.doFilter(newRequest, response)
  }
}