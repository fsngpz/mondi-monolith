package com.mondi.machine.auths.users

import com.mondi.machine.auths.roles.Role
import com.mondi.machine.auths.roles.RoleService
import com.mondi.machine.auths.verification.EmailVerificationToken
import com.mondi.machine.auths.verification.EmailVerificationTokenService
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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.OffsetDateTime
import java.util.Optional

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
    @MockitoBean
    lateinit var mockRoleService: RoleService

    @MockitoBean
    lateinit var mockUserRoleService: UserRoleService

    @MockitoBean
    lateinit var mockUserRepository: UserRepository

    @MockitoBean
    lateinit var mockPasswordEncoder: PasswordEncoder

    @MockitoBean
    lateinit var mockUserEventPublisher: UserEventPublisher

    @MockitoBean
    lateinit var mockEmailVerificationTokenService: EmailVerificationTokenService
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
        assertThat(mockEmailVerificationTokenService).isNotNull
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
        val mockUser = User("email", "pass").apply { this.id = 1L }
        val mockRole = Role("ROLE")
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "verification-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        // -- mock --
        whenever(mockUserRepository.findByEmail(any<String>())).thenReturn(Optional.empty())
        whenever(mockUserRepository.save(any<User>())).thenAnswer { invocation ->
            val user = invocation.arguments[0] as User
            user.id = 1L
            user
        }
        whenever(mockPasswordEncoder.encode(any<String>())).thenReturn("pass")
        whenever(mockRoleService.getOrCreate(any<String>(), anyOrNull())).thenReturn(mockRole)
        whenever(mockEmailVerificationTokenService.generateToken(any<User>())).thenReturn(mockToken)

        // -- execute --
        val result = service.create("email", "pass")
        assertThat(result).usingRecursiveComparison().isEqualTo(mockUser)

        // -- verify --
        verify(mockUserRepository).save(any<User>())
        verify(mockUserRoleService).assign(any<User>(), any<Role>())
        verify(mockUserEventPublisher).publish(any<UserEventRequest>())
    }
}
