package com.mondi.machine.auths

import com.mondi.machine.auths.jwt.JwtService
import com.mondi.machine.configs.CustomUserDetails
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException

/**
 * The test class for service [AuthenticationService].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
@SpringBootTest(classes = [AuthenticationService::class])
internal class AuthenticationServiceTest(@Autowired private val service: AuthenticationService) {

  // -- region of mock --
  @MockBean
  lateinit var mockJwtService: JwtService

  @MockBean
  lateinit var mockAuthenticationManager: AuthenticationManager

  // -- end of region mock --

  // -- region of smoke testing --
  @Test
  fun `dependencies are not null`() {
    assertThat(service).isNotNull
    assertThat(mockJwtService).isNotNull
    assertThat(mockAuthenticationManager).isNotNull
  }
  // -- end of region smoke testing --

  @Test
  fun `attempting to login but invalid username password`() {
    val mockAuthentication = mock<Authentication>().apply { this.isAuthenticated = false }
    // -- mock --
    whenever(
      mockAuthenticationManager.authenticate(
        any<UsernamePasswordAuthenticationToken>()
      )
    ).thenReturn(mockAuthentication)

    // -- execute --
    assertThrows<UsernameNotFoundException> { service.login("hello", "world") }

    // -- verify --
    verify(mockJwtService, never()).generateToken(any<CustomUserDetails>())
  }
}