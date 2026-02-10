package com.mondi.machine.auths.verification

import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserRepository
import com.mondi.machine.exceptions.EmailAlreadyVerifiedException
import com.mondi.machine.exceptions.EmailVerificationTokenNotFoundException
import com.mondi.machine.exceptions.TooManyRequestsException
import com.mondi.machine.utils.RateLimiterService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.OffsetDateTime
import java.util.*

/**
 * The test class of [ResendVerificationService].
 *
 * @author Ferdinand Sangap
 * @since 2026-02-10
 */
@SpringBootTest(classes = [ResendVerificationService::class])
internal class ResendVerificationServiceTest(
    @Autowired private val service: ResendVerificationService
) {
    @MockitoBean
    lateinit var mockUserRepository: UserRepository

    @MockitoBean
    lateinit var mockEmailVerificationTokenService: EmailVerificationTokenService

    @MockitoBean
    lateinit var mockRateLimiterService: RateLimiterService

    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
        assertThat(mockUserRepository).isNotNull
        assertThat(mockEmailVerificationTokenService).isNotNull
        assertThat(mockRateLimiterService).isNotNull
    }

    @Test
    fun `resendVerification successfully sends verification email`() {
        // -- arrange --
        val email = "test@example.com"
        val mockUser = User(email, "password").apply {
            this.id = 1L
            this.emailVerifiedAt = null
        }
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "new-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )

        whenever(mockUserRepository.findByEmail(email)).thenReturn(Optional.of(mockUser))
        whenever(mockEmailVerificationTokenService.generateToken(mockUser)).thenReturn(mockToken)
        whenever(mockEmailVerificationTokenService.getTokenExpiryHours()).thenReturn(24)

        // -- act --
        val response = service.resendVerification(email)

        // -- assert --
        assertThat(response.email).isEqualTo(email)
        assertThat(response.message).contains("Verification email has been sent")
        assertThat(response.expiresInHours).isEqualTo(24)

        verify(mockRateLimiterService).checkRateLimit(email)
        verify(mockUserRepository).findByEmail(email)
        verify(mockEmailVerificationTokenService).generateToken(mockUser)
        // Note: ApplicationEventPublisher interaction is tested in integration tests
    }

    @Test
    fun `resendVerification throws EmailVerificationTokenNotFoundException when user not found`() {
        // -- arrange --
        val email = "notfound@example.com"
        whenever(mockUserRepository.findByEmail(email)).thenReturn(Optional.empty())

        // -- act & assert --
        val exception = assertThrows<EmailVerificationTokenNotFoundException> {
            service.resendVerification(email)
        }
        assertThat(exception.message).contains("not found")

        verify(mockRateLimiterService).checkRateLimit(email)
        verify(mockUserRepository).findByEmail(email)
        verifyNoInteractions(mockEmailVerificationTokenService)
    }

    @Test
    fun `resendVerification throws EmailAlreadyVerifiedException when email already verified`() {
        // -- arrange --
        val email = "verified@example.com"
        val mockUser = User(email, "password").apply {
            this.id = 2L
            this.emailVerifiedAt = OffsetDateTime.now().minusDays(1)
        }

        whenever(mockUserRepository.findByEmail(email)).thenReturn(Optional.of(mockUser))

        // -- act & assert --
        val exception = assertThrows<EmailAlreadyVerifiedException> {
            service.resendVerification(email)
        }
        assertThat(exception.message).contains("already verified")

        verify(mockRateLimiterService).checkRateLimit(email)
        verify(mockUserRepository).findByEmail(email)
        verifyNoInteractions(mockEmailVerificationTokenService)
    }

    @Test
    fun `resendVerification throws TooManyRequestsException when rate limit exceeded`() {
        // -- arrange --
        val email = "ratelimited@example.com"
        doThrow(TooManyRequestsException("Too many requests"))
            .whenever(mockRateLimiterService).checkRateLimit(email)

        // -- act & assert --
        val exception = assertThrows<TooManyRequestsException> {
            service.resendVerification(email)
        }
        assertThat(exception.message).contains("Too many requests")

        verify(mockRateLimiterService).checkRateLimit(email)
        verifyNoInteractions(mockUserRepository)
        verifyNoInteractions(mockEmailVerificationTokenService)
    }

    @Test
    fun `resendVerification checks rate limit before any processing`() {
        // -- arrange --
        val email = "order@example.com"
        doThrow(TooManyRequestsException("Rate limit exceeded"))
            .whenever(mockRateLimiterService).checkRateLimit(email)

        // -- act & assert --
        assertThrows<TooManyRequestsException> {
            service.resendVerification(email)
        }

        // Verify rate limit is checked first
        val inOrder = inOrder(mockRateLimiterService, mockUserRepository)
        inOrder.verify(mockRateLimiterService).checkRateLimit(email)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `resendVerification generates new token invalidating old ones`() {
        // -- arrange --
        val email = "newtoken@example.com"
        val mockUser = User(email, "password").apply {
            this.id = 3L
            this.emailVerifiedAt = null
        }
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "brand-new-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )

        whenever(mockUserRepository.findByEmail(email)).thenReturn(Optional.of(mockUser))
        whenever(mockEmailVerificationTokenService.generateToken(mockUser)).thenReturn(mockToken)
        whenever(mockEmailVerificationTokenService.getTokenExpiryHours()).thenReturn(24)

        // -- act --
        service.resendVerification(email)

        // -- assert --
        // generateToken should invalidate old tokens internally
        verify(mockEmailVerificationTokenService).generateToken(mockUser)
    }
}
