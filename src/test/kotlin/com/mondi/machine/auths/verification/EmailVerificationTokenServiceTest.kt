package com.mondi.machine.auths.verification

import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRepository
import com.mondi.machine.exceptions.EmailVerificationTokenNotFoundException
import com.mondi.machine.exceptions.InvalidEmailVerificationTokenException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.OffsetDateTime
import java.util.*

/**
 * The test class of [EmailVerificationTokenService].
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
@SpringBootTest(classes = [EmailVerificationTokenService::class])
@TestPropertySource(properties = ["app.email-verification.token-expiry-hours=24"])
internal class EmailVerificationTokenServiceTest(
    @Autowired private val service: EmailVerificationTokenService
) {
    // -- region of mock --
    @MockitoBean
    lateinit var mockTokenRepository: EmailVerificationTokenRepository

    @MockitoBean
    lateinit var mockUserRepository: UserRepository
    // -- end of region mock --

    // -- region of smoke test --
    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
        assertThat(mockTokenRepository).isNotNull
        assertThat(mockUserRepository).isNotNull
    }
    // -- end of region smoke test --

    @Test
    fun `generateToken creates new token for user`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }
        whenever(mockTokenRepository.findValidTokensByUser(any(), any())).thenReturn(emptyList())
        whenever(mockTokenRepository.save(any<EmailVerificationToken>())).thenAnswer { it.arguments[0] }

        // -- act --
        val token = service.generateToken(mockUser)

        // -- assert --
        assertThat(token.user).isEqualTo(mockUser)
        assertThat(token.token).isNotEmpty()
        assertThat(token.expiresAt).isAfter(OffsetDateTime.now())
        assertThat(token.verifiedAt).isNull()
        verify(mockTokenRepository).save(any<EmailVerificationToken>())
    }

    @Test
    fun `generateToken invalidates existing valid tokens before creating new one`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }
        val existingToken = EmailVerificationToken(
            user = mockUser,
            token = "old-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        whenever(mockTokenRepository.findValidTokensByUser(any(), any())).thenReturn(listOf(existingToken))
        whenever(mockTokenRepository.save(any<EmailVerificationToken>())).thenAnswer { it.arguments[0] }
        whenever(mockTokenRepository.saveAll(any<List<EmailVerificationToken>>())).thenReturn(listOf(existingToken))

        // -- act --
        service.generateToken(mockUser)

        // -- assert --
        assertThat(existingToken.verifiedAt).isNotNull()
        verify(mockTokenRepository).saveAll(any<List<EmailVerificationToken>>())
        verify(mockTokenRepository).save(any<EmailVerificationToken>())
    }

    @Test
    fun `verifyEmail successfully verifies valid token`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply {
            this.id = 1L
            this.emailVerifiedAt = null
        }
        val token = EmailVerificationToken(
            user = mockUser,
            token = "valid-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        whenever(mockTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token))
        whenever(mockTokenRepository.save(any<EmailVerificationToken>())).thenReturn(token)
        whenever(mockUserRepository.save(any<User>())).thenReturn(mockUser)

        // -- act --
        val result = service.verifyEmail("valid-token")

        // -- assert --
        assertThat(result).isEqualTo(mockUser)
        assertThat(token.verifiedAt).isNotNull()
        assertThat(mockUser.emailVerifiedAt).isNotNull()
        verify(mockTokenRepository).save(token)
        verify(mockUserRepository).save(mockUser)
    }

    @Test
    fun `verifyEmail throws EmailVerificationTokenNotFoundException when token not found`() {
        // -- arrange --
        whenever(mockTokenRepository.findByToken("non-existent")).thenReturn(Optional.empty())

        // -- act & assert --
        val exception = assertThrows<EmailVerificationTokenNotFoundException> {
            service.verifyEmail("non-existent")
        }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `verifyEmail throws InvalidEmailVerificationTokenException when token is expired`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }
        val expiredToken = EmailVerificationToken(
            user = mockUser,
            token = "expired-token",
            expiresAt = OffsetDateTime.now().minusHours(1)
        )
        whenever(mockTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken))

        // -- act & assert --
        val exception = assertThrows<InvalidEmailVerificationTokenException> {
            service.verifyEmail("expired-token")
        }
        assertThat(exception.message).contains("expired")
    }

    @Test
    fun `verifyEmail throws InvalidEmailVerificationTokenException when token already used`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }
        val usedToken = EmailVerificationToken(
            user = mockUser,
            token = "used-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        ).apply {
            this.verifiedAt = OffsetDateTime.now().minusHours(1)
        }
        whenever(mockTokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken))

        // -- act & assert --
        val exception = assertThrows<InvalidEmailVerificationTokenException> {
            service.verifyEmail("used-token")
        }
        assertThat(exception.message).contains("already been used")
    }

    @Test
    fun `verifyEmail does not update user if already verified`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply {
            this.id = 1L
            this.emailVerifiedAt = OffsetDateTime.now().minusDays(1)
        }
        val token = EmailVerificationToken(
            user = mockUser,
            token = "valid-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        whenever(mockTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token))
        whenever(mockTokenRepository.save(any<EmailVerificationToken>())).thenReturn(token)

        // -- act --
        val result = service.verifyEmail("valid-token")

        // -- assert --
        assertThat(result).isEqualTo(mockUser)
        assertThat(token.verifiedAt).isNotNull()
        verify(mockTokenRepository).save(token)
        verify(mockUserRepository, never()).save(any())
    }

    @Test
    fun `invalidateUserTokens marks all valid tokens as verified`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }
        val token1 = EmailVerificationToken(
            user = mockUser,
            token = "token1",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        val token2 = EmailVerificationToken(
            user = mockUser,
            token = "token2",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        whenever(mockTokenRepository.findValidTokensByUser(any(), any())).thenReturn(listOf(token1, token2))
        whenever(mockTokenRepository.saveAll(any<List<EmailVerificationToken>>())).thenReturn(listOf(token1, token2))

        // -- act --
        service.invalidateUserTokens(mockUser)

        // -- assert --
        assertThat(token1.verifiedAt).isNotNull()
        assertThat(token2.verifiedAt).isNotNull()
        verify(mockTokenRepository).saveAll(any<List<EmailVerificationToken>>())
    }

    @Test
    fun `deleteUserTokens deletes all tokens for user`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }

        // -- act --
        service.deleteUserTokens(mockUser)

        // -- assert --
        verify(mockTokenRepository).deleteByUser(mockUser)
    }

    @Test
    fun `cleanupExpiredTokens removes expired tokens`() {
        // -- arrange --
        whenever(mockTokenRepository.deleteExpiredTokens(any())).thenReturn(5)

        // -- act --
        val deletedCount = service.cleanupExpiredTokens()

        // -- assert --
        assertThat(deletedCount).isEqualTo(5)
        verify(mockTokenRepository).deleteExpiredTokens(any())
    }

    @Test
    fun `hasValidToken returns true when user has valid tokens`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }
        whenever(mockTokenRepository.existsValidTokenForUser(any(), any())).thenReturn(true)

        // -- act --
        val hasValid = service.hasValidToken(mockUser)

        // -- assert --
        assertThat(hasValid).isTrue()
        verify(mockTokenRepository).existsValidTokenForUser(any(), any())
    }

    @Test
    fun `hasValidToken returns false when user has no valid tokens`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }
        whenever(mockTokenRepository.existsValidTokenForUser(any(), any())).thenReturn(false)

        // -- act --
        val hasValid = service.hasValidToken(mockUser)

        // -- assert --
        assertThat(hasValid).isFalse()
        verify(mockTokenRepository).existsValidTokenForUser(any(), any())
    }

    @Test
    fun `getTokenExpiryHours returns configured value`() {
        // -- act --
        val expiryHours = service.getTokenExpiryHours()

        // -- assert --
        assertThat(expiryHours).isEqualTo(24)
    }
}
