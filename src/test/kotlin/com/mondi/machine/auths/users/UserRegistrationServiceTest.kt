package com.mondi.machine.auths.users

import com.mondi.machine.auths.roles.Role
import com.mondi.machine.auths.roles.RoleService
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * The test class for [UserRegistrationService].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
@SpringBootTest(classes = [UserRegistrationService::class])
internal class UserRegistrationServiceTest(
  @Autowired private val service: UserRegistrationService
) {
  // -- region of mock --
  @MockBean
  lateinit var mockRoleService: RoleService

  @MockBean
  lateinit var mockUserRoleService: UserRoleService

  @MockBean
  lateinit var mockUserRepository: UserRepository

  @MockBean
  lateinit var mockPasswordEncoder: PasswordEncoder

  @MockBean
  lateinit var mockUserEventPublisher: UserEventPublisher
  // -- end of region mock --

  // -- region of smoke testing --
  @Test
  fun `dependencies are not null`() {
    assertThat(service).isNotNull
    assertThat(mockRoleService).isNotNull
    assertThat(mockUserRoleService).isNotNull
    assertThat(mockUserRepository).isNotNull
    assertThat(mockPasswordEncoder).isNotNull
    assertThat(mockUserEventPublisher).isNotNull
  }
  // -- end of region smoke testing --

  @Test
  fun `attempting to create but email is already exist`() {
    val mockUser = User("email", "pass")
    // -- mock --
    whenever(mockUserRepository.findByEmail(any<String>())).thenReturn(Optional.of(mockUser))

    // -- execute --
    assertThrows<DataIntegrityViolationException> { service.create("email", "pass") }

    // -- verify --
    verify(mockUserRepository, never()).save(any<User>())
  }

  @Test
  fun `attempting to create then success`() {
    val mockUser = User("email", "pass")
    val mockRole = Role("ROLE")
    // -- mock --
    whenever(mockUserRepository.findByEmail(any<String>())).thenReturn(Optional.empty())
    whenever(mockUserRepository.save(any<User>())).thenReturn(mockUser)
    whenever(mockPasswordEncoder.encode(any<String>())).thenReturn("pass")
    whenever(mockRoleService.getOrCreate(any<String>(), anyOrNull())).thenReturn(mockRole)

    // -- execute --
    val result = service.create("email", "pass")
    assertThat(result).usingRecursiveComparison().isEqualTo(mockUser)

    // -- verify --
    verify(mockUserRepository).save(any<User>())
    verify(mockUserRoleService).assign(any<User>(), any<Role>())
    verify(mockUserEventPublisher).publish(any<UserEventRequest>())
  }
}