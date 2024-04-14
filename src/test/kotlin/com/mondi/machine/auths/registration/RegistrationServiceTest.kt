package com.mondi.machine.auths.registration

import com.mondi.machine.auths.users.UserRegistrationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

/**
 * The test class of [RegistrationService].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
@SpringBootTest(classes = [RegistrationService::class])
internal class RegistrationServiceTest(@Autowired private val service: RegistrationService) {
  // -- region of mock --
  @MockBean
  lateinit var mockUserRegistrationService: UserRegistrationService
  // -- end of region mock --

  // -- region of smoke testing --
  @Test
  fun `dependencies are not null`() {
    assertThat(service).isNotNull
    assertThat(mockUserRegistrationService).isNotNull
  }
  // -- end of region smoke testing --

  @Test
  fun `attempting to register but email is null`() {
    // -- mock --
    val mockRequest = RegistrationRequest(null, "pass")

    // -- execute --
    assertThrows<IllegalArgumentException> { service.register(mockRequest) }

    // -- verify --
    verify(mockUserRegistrationService, never()).create(any<String>(), any<String>())
  }

  @Test
  fun `attempting to register but password is null`() {
    // -- mock --
    val mockRequest = RegistrationRequest("mail.com", null)

    // -- execute --
    assertThrows<IllegalArgumentException> { service.register(mockRequest) }

    // -- verify --
    verify(mockUserRegistrationService, never()).create(any<String>(), any<String>())
  }

  @Test
  fun `attempting to register and success`() {
    // -- mock --
    val mockRequest = RegistrationRequest("mail.com", "pass")

    // -- execute --
    service.register(mockRequest)

    // -- verify --
    verify(mockUserRegistrationService).create(any<String>(), any<String>())
  }
}