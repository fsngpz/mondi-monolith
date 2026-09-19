package com.mondi.machine.notifications.emails

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import com.mondi.machine.auths.verification.EmailVerificationToken
import com.mondi.machine.auths.verification.EmailVerificationTokenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.OffsetDateTime

/**
 * The test class of [EmailVerificationEventListener].
 *
 * @author Ferdinand Sangap.
 * @since 2026-02-09
 */
@SpringBootTest(classes = [EmailVerificationEventListener::class])
@TestPropertySource(
    properties = [
        "app.url.base=http://localhost:9000"
    ]
)
internal class EmailVerificationEventListenerTest(
    @Autowired private val listener: EmailVerificationEventListener
) {
    // -- region of mock --
    @MockitoBean
    lateinit var mockEmailService: EmailService

    @MockitoBean
    lateinit var mockVerificationTokenService: EmailVerificationTokenService
    // -- end of region mock --

    // -- region of smoke test --
    @Test
    fun `dependencies are not null`() {
        assertThat(listener).isNotNull
        assertThat(mockEmailService).isNotNull
        assertThat(mockVerificationTokenService).isNotNull
    }
    // -- end of region smoke test --

    @Test
    fun `onApplicationEvent sends verification email with user profile name`() {
        // -- arrange --
        val mockUser = User("john@example.com", "password").apply {
            this.id = 1L
            this.emailVerifiedAt = null
            this.profile = Profile(this).apply {
                this.name = "John"
            }
        }
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "test-token-123",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        whenever(mockVerificationTokenService.generateToken(mockUser)).thenReturn(mockToken)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
        verify(mockVerificationTokenService).generateToken(mockUser)
        verify(mockEmailService).sendTemplateEmail(
            to = eq("john@example.com"),
            subject = eq("Verify Your Email - Mondi Jewellery"),
            templateName = eq(EmailTemplateNames.EMAIL_VERIFICATION),
            context = argThat { context ->
                context["USER_NAME"] == "John" &&
                        context.containsKey("VERIFICATION_URL") &&
                        (context["VERIFICATION_URL"] as String) == "http://localhost:9000/verify-email?token=test-token-123"
            }
        )
    }

    @Test
    fun `onApplicationEvent sends verification email with email username when profile is null`() {
        // -- arrange --
        val mockUser = User("jane.smith@example.com", "password").apply {
            this.id = 2L
            this.emailVerifiedAt = null
            this.profile = null
        }
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "test-token-456",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        whenever(mockVerificationTokenService.generateToken(mockUser)).thenReturn(mockToken)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
        verify(mockVerificationTokenService).generateToken(mockUser)
        verify(mockEmailService).sendTemplateEmail(
            to = eq("jane.smith@example.com"),
            subject = any(),
            templateName = eq(EmailTemplateNames.EMAIL_VERIFICATION),
            context = argThat { context ->
                context["USER_NAME"] == "jane.smith"
            }
        )
    }

    @Test
    fun `onApplicationEvent skips verification email when email is already verified`() {
        // -- arrange --
        val mockUser = User("verified@example.com", "password").apply {
            this.id = 3L
            this.emailVerifiedAt = OffsetDateTime.now()
        }
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
        verifyNoInteractions(mockEmailService)
    }

    @Test
    fun `onApplicationEvent generates unique verification token`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply {
            this.id = 4L
            this.emailVerifiedAt = null
        }
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "unique-token-789",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        whenever(mockVerificationTokenService.generateToken(mockUser)).thenReturn(mockToken)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
        verify(mockVerificationTokenService).generateToken(mockUser)
        verify(mockEmailService).sendTemplateEmail(
            to = any(),
            subject = any(),
            templateName = any(),
            context = argThat { context ->
                val verificationUrl = context["VERIFICATION_URL"] as String
                verificationUrl.contains("token=unique-token-789")
            }
        )
    }

    @Test
    fun `onApplicationEvent handles email service exception gracefully`() {
        // -- arrange --
        val mockUser = User("error@example.com", "password").apply {
            this.id = 5L
            this.emailVerifiedAt = null
        }
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "error-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        whenever(mockVerificationTokenService.generateToken(mockUser)).thenReturn(mockToken)
        whenever(mockEmailService.sendTemplateEmail(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Email service failure"))

        // -- act & assert --
        // Should not throw exception - error is logged and swallowed
        listener.onApplicationEvent(mockEvent)

        verify(mockVerificationTokenService).generateToken(mockUser)
        verify(mockEmailService).sendTemplateEmail(any(), any(), any(), any())
    }

    @Test
    fun `onApplicationEvent includes all required context variables`() {
        // -- arrange --
        val mockUser = User("complete@example.com", "password").apply {
            this.id = 6L
            this.emailVerifiedAt = null
            this.profile = Profile(this).apply {
                this.name = "Complete"
            }
        }
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "complete-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        whenever(mockVerificationTokenService.generateToken(mockUser)).thenReturn(mockToken)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
        verify(mockVerificationTokenService).generateToken(mockUser)
        verify(mockEmailService).sendTemplateEmail(
            to = any(),
            subject = any(),
            templateName = any(),
            context = argThat { context ->
                context.containsKey("USER_NAME") &&
                        context.containsKey("VERIFICATION_URL") &&
                        context.size == 2
            }
        )
    }
}
