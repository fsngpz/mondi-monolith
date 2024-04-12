package com.mondi.machine.auths.users

import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

/**
 * The test class of [UserService].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
@SpringBootTest(classes = [UserService::class])
internal class UserServiceTest(@Autowired private val service: UserService) {
  // -- region of mock --
  @MockBean
  lateinit var mockRepository: UserRepository
  // -- end of region mock --

  // -- region of smoke testing --
  @Test
  fun `dependencies are not null`() {
    assertThat(service).isNotNull
    assertThat(mockRepository).isNotNull
  }
  // -- end of region smoke testing --

  @Test
  fun `get the instance by email but not found`() {
    // -- mock --
    whenever(mockRepository.findByEmail(any<String>())).thenReturn(Optional.empty())

    // -- execute --
    val result = service.getByEmail("email")
    assertThat(result).isEmpty

    // -- verify --
    verify(mockRepository).findByEmail(any<String>())
  }

  @Test
  fun `get the instance by email and found`() {
    val mockUser = User("hello", "world")
    // -- mock --
    whenever(mockRepository.findByEmail(any<String>())).thenReturn(Optional.of(mockUser))

    // -- execute --
    val result = service.getByEmail("email")
    assertThat(result).isPresent

    // -- verify --
    verify(mockRepository).findByEmail(any<String>())
  }

  @Test
  fun `is email exist, true`() {
    val mockUser = User("hello", "world")
    // -- mock --
    whenever(mockRepository.findByEmail(any<String>())).thenReturn(Optional.of(mockUser))

    // -- execute --
    val result = service.isEmailAlreadyExist("email")
    assertThat(result).isTrue

    // -- verify --
    verify(mockRepository).findByEmail(any<String>())
  }

  @Test
  fun `is email exist, false`() {
    val mockUser = User("hello", "world")
    // -- mock --
    whenever(mockRepository.findByEmail(any<String>())).thenReturn(Optional.empty())

    // -- execute --
    val result = service.isEmailAlreadyExist("email")
    assertThat(result).isFalse

    // -- verify --
    verify(mockRepository).findByEmail(any<String>())
  }
}