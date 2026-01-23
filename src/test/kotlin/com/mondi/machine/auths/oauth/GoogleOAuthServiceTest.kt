package com.mondi.machine.auths.oauth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.mondi.machine.auths.jwt.JwtService
import com.mondi.machine.auths.roles.Role
import com.mondi.machine.auths.roles.RoleService
import com.mondi.machine.auths.users.OAuthProvider
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserEventPublisher
import com.mondi.machine.auths.users.UserRepository
import com.mondi.machine.auths.users.UserRoleService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * The test class for [GoogleOAuthService].
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@SpringBootTest(classes = [GoogleOAuthService::class])
@ActiveProfiles("test")
internal class GoogleOAuthServiceTest(
    @Autowired private val googleOAuthService: GoogleOAuthService
) {
    @MockitoBean
    private lateinit var mockGoogleOAuthProperties: GoogleOAuthProperties

    @MockitoBean
    private lateinit var mockJwtService: JwtService

    @MockitoBean
    private lateinit var mockUserRepository: UserRepository

    @MockitoBean
    private lateinit var mockRoleService: RoleService

    @MockitoBean
    private lateinit var mockUserRoleService: UserRoleService

    @MockitoBean
    private lateinit var mockUserEventPublisher: UserEventPublisher

    @Test
    fun `dependencies are not null`() {
        assertThat(googleOAuthService).isNotNull
        assertThat(mockGoogleOAuthProperties).isNotNull
        assertThat(mockJwtService).isNotNull
        assertThat(mockUserRepository).isNotNull
        assertThat(mockRoleService).isNotNull
        assertThat(mockUserRoleService).isNotNull
        assertThat(mockUserEventPublisher).isNotNull
    }

    @Test
    fun `authenticate with valid token for new user`() {
        val idToken = "valid-google-id-token"
        val email = "test@gmail.com"
        val providerId = "google-user-id-123"
        val name = "Test User"

        // -- create mock payload --
        val mockPayload = GoogleIdToken.Payload().apply {
            this.email = email
            this.subject = providerId
            this["name"] = name
        }

        // -- create mock user and role --
        val mockRole = createMockRole()

        // -- mock GoogleOAuthService.verifyGoogleToken --
        val spyService = org.mockito.kotlin.spy(googleOAuthService)
        whenever(spyService.verifyGoogleToken(idToken)).thenReturn(mockPayload)

        // -- mock repository and services --
        whenever(mockUserRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, providerId)).thenReturn(null)
        whenever(mockUserRepository.findByEmail(email)).thenReturn(java.util.Optional.empty())
        whenever(mockUserRepository.save(any<User>())).thenAnswer { invocation ->
            val user = invocation.arguments[0] as User
            user.id = 1L
            user
        }
        whenever(mockRoleService.getOrCreate(GoogleOAuthService.ROLE_USER)).thenReturn(mockRole)
        whenever(mockJwtService.generateToken(any())).thenReturn("jwt-token")

        // -- execute --
        val result = spyService.authenticate(idToken)

        // -- assertions --
        assertThat(result).isNotNull
        assertThat(result.bearerToken).isEqualTo("jwt-token")

        // -- verify interactions --
        verify(mockUserRepository).save(any<User>())
        verify(mockRoleService).getOrCreate(GoogleOAuthService.ROLE_USER)
        verify(mockUserRoleService).assign(any(), any())
        verify(mockUserEventPublisher).publish(any())
    }

    @Test
    fun `authenticate with valid token for existing user`() {
        val idToken = "valid-google-id-token"
        val email = "test@gmail.com"
        val providerId = "google-user-id-123"
        val name = "Test User"

        // -- create mock payload --
        val mockPayload = GoogleIdToken.Payload().apply {
            this.email = email
            this.subject = providerId
            this["name"] = name
        }

        // -- create mock user --
        val mockUser = createMockUser(email, providerId)

        // -- mock GoogleOAuthService.verifyGoogleToken --
        val spyService = org.mockito.kotlin.spy(googleOAuthService)
        whenever(spyService.verifyGoogleToken(idToken)).thenReturn(mockPayload)

        // -- mock repository --
        whenever(mockUserRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, providerId)).thenReturn(mockUser)
        whenever(mockJwtService.generateToken(any())).thenReturn("jwt-token")

        // -- execute --
        val result = spyService.authenticate(idToken)

        // -- assertions --
        assertThat(result).isNotNull
        assertThat(result.bearerToken).isEqualTo("jwt-token")

        // -- verify no new user is created --
        verify(mockUserRepository, org.mockito.kotlin.never()).save(any<User>())
    }

    @Test
    fun `authenticate with invalid token throws exception`() {
        val idToken = "invalid-google-id-token"

        // -- mock GoogleOAuthService.verifyGoogleToken to return null --
        val spyService = org.mockito.kotlin.spy(googleOAuthService)
        whenever(spyService.verifyGoogleToken(idToken)).thenReturn(null)

        // -- execute and assert --
        val exception = assertThrows<IllegalArgumentException> {
            spyService.authenticate(idToken)
        }

        assertThat(exception.message).isEqualTo("Invalid Google ID token")
    }

    @Test
    fun `authenticate with email already exists locally throws exception`() {
        val idToken = "valid-google-id-token"
        val email = "test@gmail.com"
        val providerId = "google-user-id-123"
        val name = "Test User"

        // -- create mock payload --
        val mockPayload = GoogleIdToken.Payload().apply {
            this.email = email
            this.subject = providerId
            this["name"] = name
        }

        // -- create mock local user --
        val mockLocalUser = User(email, "password", OAuthProvider.LOCAL, null).apply {
            this.id = 1L
        }

        // -- mock GoogleOAuthService.verifyGoogleToken --
        val spyService = org.mockito.kotlin.spy(googleOAuthService)
        whenever(spyService.verifyGoogleToken(idToken)).thenReturn(mockPayload)

        // -- mock repository --
        whenever(mockUserRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, providerId)).thenReturn(null)
        whenever(mockUserRepository.findByEmail(email)).thenReturn(java.util.Optional.of(mockLocalUser))

        // -- execute and assert --
        val exception = assertThrows<IllegalArgumentException> {
            spyService.authenticate(idToken)
        }

        assertThat(exception.message).contains("already exists")
    }

    private fun createMockUser(email: String, providerId: String): User {
        return User(
            email = email,
            password = null,
            provider = OAuthProvider.GOOGLE,
            providerId = providerId
        ).apply {
            this.id = 1L
            this.username = "Test User"
        }
    }

    private fun createMockRole(): Role {
        return Role(name = GoogleOAuthService.ROLE_USER).apply {
            this.id = 1L
            this.description = "User role"
        }
    }
}
