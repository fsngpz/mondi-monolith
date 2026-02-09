package com.mondi.machine.notifications.emails

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
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
    // -- end of region mock --

    // -- region of smoke test --
    @Test
    fun `dependencies are not null`() {
        assertThat(listener).isNotNull
        assertThat(mockEmailService).isNotNull
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
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
        verify(mockEmailService).sendTemplateEmail(
            to = eq("john@example.com"),
            subject = eq("Verify Your Email - Mondi Jewellery"),
            templateName = eq(EmailTemplateNames.EMAIL_VERIFICATION),
            context = argThat { context ->
                context["USER_NAME"] == "John" &&
                        context.containsKey("VERIFICATION_URL") &&
                        (context["VERIFICATION_URL"] as String).startsWith("http://localhost:9000/api/auth/verify-email?token=") &&
                        (context["VERIFICATION_URL"] as String).contains("&email=john@example.com")
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
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
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
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
        verify(mockEmailService).sendTemplateEmail(
            to = any(),
            subject = any(),
            templateName = any(),
            context = argThat { context ->
                val verificationUrl = context["VERIFICATION_URL"] as String
                verificationUrl.contains("token=") &&
                        verificationUrl.split("token=")[1].split("&")[0].isNotEmpty()
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
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        whenever(mockEmailService.sendTemplateEmail(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Email service failure"))

        // -- act & assert --
        // Should not throw exception - error is logged and swallowed
        listener.onApplicationEvent(mockEvent)

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
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
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
