package com.mondi.machine.auths.verification

import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserService
import com.mondi.machine.exceptions.EmailAlreadyVerifiedException
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
    lateinit var mockUserService: UserService

    @MockitoBean
    lateinit var mockEmailVerificationTokenService: EmailVerificationTokenService

    @MockitoBean
    lateinit var mockRateLimiterService: RateLimiterService

    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
        assertThat(mockUserService).isNotNull
        assertThat(mockEmailVerificationTokenService).isNotNull
        assertThat(mockRateLimiterService).isNotNull
    }

    @Test
    fun `resendVerification successfully sends verification email`() {
        // -- arrange --
        val userId = 1L
        val email = "test@example.com"
        val mockUser = User(email, "password").apply {
            this.id = userId
            this.emailVerifiedAt = null
        }
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "new-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )

        whenever(mockUserService.get(userId)).thenReturn(mockUser)
        whenever(mockEmailVerificationTokenService.generateToken(mockUser)).thenReturn(mockToken)
        whenever(mockEmailVerificationTokenService.getTokenExpiryHours()).thenReturn(24)

        // -- act --
        val response = service.resendVerification(userId)

        // -- assert --
        assertThat(response.email).isEqualTo(email)
        assertThat(response.message).contains("Verification email has been sent")
        assertThat(response.expiresInHours).isEqualTo(24)

        verify(mockUserService).get(userId)
        verify(mockRateLimiterService).checkRateLimit(email)
        verify(mockEmailVerificationTokenService).generateToken(mockUser)
        // Note: ApplicationEventPublisher interaction is tested in integration tests
    }

    @Test
    fun `resendVerification throws NoSuchElementException when user not found`() {
        // -- arrange --
        val userId = 999L
        whenever(mockUserService.get(userId)).thenThrow(NoSuchElementException("no user was found with id '$userId'"))

        // -- act & assert --
        val exception = assertThrows<NoSuchElementException> {
            service.resendVerification(userId)
        }
        assertThat(exception.message).contains("no user was found")

        verify(mockUserService).get(userId)
        verifyNoInteractions(mockRateLimiterService)
        verifyNoInteractions(mockEmailVerificationTokenService)
    }

    @Test
    fun `resendVerification throws EmailAlreadyVerifiedException when email already verified`() {
        // -- arrange --
        val userId = 2L
        val email = "verified@example.com"
        val mockUser = User(email, "password").apply {
            this.id = userId
            this.emailVerifiedAt = OffsetDateTime.now().minusDays(1)
        }

        whenever(mockUserService.get(userId)).thenReturn(mockUser)

        // -- act & assert --
        val exception = assertThrows<EmailAlreadyVerifiedException> {
            service.resendVerification(userId)
        }
        assertThat(exception.message).contains("already verified")

        verify(mockUserService).get(userId)
        verify(mockRateLimiterService).checkRateLimit(email)
        verifyNoInteractions(mockEmailVerificationTokenService)
    }

    @Test
    fun `resendVerification throws TooManyRequestsException when rate limit exceeded`() {
        // -- arrange --
        val userId = 3L
        val email = "ratelimited@example.com"
        val mockUser = User(email, "password").apply {
            this.id = userId
            this.emailVerifiedAt = null
        }

        whenever(mockUserService.get(userId)).thenReturn(mockUser)
        doThrow(TooManyRequestsException("Too many requests"))
            .whenever(mockRateLimiterService).checkRateLimit(email)

        // -- act & assert --
        val exception = assertThrows<TooManyRequestsException> {
            service.resendVerification(userId)
        }
        assertThat(exception.message).contains("Too many requests")

        verify(mockUserService).get(userId)
        verify(mockRateLimiterService).checkRateLimit(email)
        verifyNoInteractions(mockEmailVerificationTokenService)
    }

    @Test
    fun `resendVerification checks operations in correct order`() {
        // -- arrange --
        val userId = 4L
        val email = "order@example.com"
        val mockUser = User(email, "password").apply {
            this.id = userId
            this.emailVerifiedAt = null
        }

        whenever(mockUserService.get(userId)).thenReturn(mockUser)
        doThrow(TooManyRequestsException("Rate limit exceeded"))
            .whenever(mockRateLimiterService).checkRateLimit(email)

        // -- act & assert --
        assertThrows<TooManyRequestsException> {
            service.resendVerification(userId)
        }

        // Verify operations are in correct order: get user -> check rate limit
        val inOrder = inOrder(mockUserService, mockRateLimiterService)
        inOrder.verify(mockUserService).get(userId)
        inOrder.verify(mockRateLimiterService).checkRateLimit(email)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `resendVerification generates new token invalidating old ones`() {
        // -- arrange --
        val userId = 5L
        val email = "newtoken@example.com"
        val mockUser = User(email, "password").apply {
            this.id = userId
            this.emailVerifiedAt = null
        }
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "brand-new-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )

        whenever(mockUserService.get(userId)).thenReturn(mockUser)
        whenever(mockEmailVerificationTokenService.generateToken(mockUser)).thenReturn(mockToken)
        whenever(mockEmailVerificationTokenService.getTokenExpiryHours()).thenReturn(24)

        // -- act --
        service.resendVerification(userId)

        // -- assert --
        // generateToken should invalidate old tokens internally
        verify(mockEmailVerificationTokenService).generateToken(mockUser)
    }
}
