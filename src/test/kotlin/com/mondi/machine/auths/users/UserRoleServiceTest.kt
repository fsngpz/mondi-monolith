package com.mondi.machine.auths.users

import com.mondi.machine.auths.roles.Role
import com.sun.jdi.request.DuplicateRequestException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

/**
 * The test class of [UserRoleService].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
@SpringBootTest(classes = [UserRoleService::class])
internal class UserRoleServiceTest(@Autowired private val service: UserRoleService) {
  // -- region of mock --
  @MockBean
  lateinit var mockRepository: UserRoleRepository
  // -- end of region mock --

  // -- region of smoke testing --
  @Test
  fun `dependencies are not null`() {
    assertThat(service).isNotNull
    assertThat(mockRepository).isNotNull
  }
  // -- end of region smoke testing --

  @Test
  fun `assigning the role but was duplicated`() {
    val mockUser = User("email", "pass")
    val mockRole = Role("name")
    val mockUserRole = UserRole(mockUser, mockRole)
    // -- mock --
    whenever(mockRepository.findByUserAndRole(any<User>(), any<Role>())).thenReturn(mockUserRole)

    // -- execute --
    assertThrows<DuplicateRequestException> { service.assign(mockUser, mockRole) }

    // -- verify --
    verify(mockRepository).findByUserAndRole(any<User>(), any<Role>())
    verify(mockRepository, never()).save(any<UserRole>())
  }

  @Test
  fun `assigning the role and success`() {
    val mockUser = User("email", "pass")
    val mockRole = Role("name")
    // -- mock --
    whenever(mockRepository.findByUserAndRole(any<User>(), any<Role>())).thenReturn(null)

    // -- execute --
    service.assign(mockUser, mockRole)

    // -- verify --
    verify(mockRepository).findByUserAndRole(any<User>(), any<Role>())
    verify(mockRepository).save(any<UserRole>())
  }
}