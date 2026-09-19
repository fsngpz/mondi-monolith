package com.mondi.machine.notifications.emails

import com.mondi.machine.accounts.profiles.ProfileService
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import com.mondi.machine.auths.verification.EmailVerificationToken
import com.mondi.machine.auths.verification.EmailVerificationTokenService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.OffsetDateTime

/**
 * Integration test class for email notification flow.
 *
 * Tests the complete flow from event publishing to email sending,
 * ensuring all listeners are properly wired and functioning together.
 *
 * @author Ferdinand Sangap.
 * @since 2026-02-09
 */
@SpringBootTest
@TestPropertySource(
    properties = [
        "app.url.base=http://localhost:9000",
        "app.url.collections=https://mondijewellery.studio/collections",
        "app.url.instagram=https://instagram.com/mondijewellery",
        "app.url.tiktok=https://tiktok.com/@mondijewellery"
    ]
)
internal class EmailNotificationIntegrationTest {

    @Autowired
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @MockitoBean
    private lateinit var mockEmailService: EmailService

    @MockitoBean
    private lateinit var mockVerificationTokenService: EmailVerificationTokenService

    @MockitoBean
    private lateinit var mockProfileService: ProfileService

    @Test
    fun `publishing UserApplicationEvent triggers welcome email`() {
        // -- arrange --
        val mockUser = User("welcome@example.com", "password")
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "test-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        whenever(mockVerificationTokenService.generateToken(any())).thenReturn(mockToken)

        val userEventRequest = UserEventRequest(mockUser)
        val event = UserApplicationEvent(userEventRequest)

        // -- act --
        applicationEventPublisher.publishEvent(event)

        // -- give listeners time to process (asynchronous handling if needed) --
        Thread.sleep(100)

        // -- assert --
        verify(mockEmailService, atLeastOnce()).sendTemplateEmail(
            to = eq("welcome@example.com"),
            subject = argThat { subject -> subject.contains("Welcome") },
            templateName = eq(EmailTemplateNames.WELCOME),
            context = any()
        )
    }

    @Test
    fun `publishing UserApplicationEvent triggers verification email for unverified user`() {
        // -- arrange --
        val mockUser = User("verify@example.com", "password")
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "verify-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        whenever(mockVerificationTokenService.generateToken(any())).thenReturn(mockToken)

        val userEventRequest = UserEventRequest(mockUser)
        val event = UserApplicationEvent(userEventRequest)

        // -- act --
        applicationEventPublisher.publishEvent(event)

        // -- give listeners time to process --
        Thread.sleep(100)

        // -- assert --
        verify(mockEmailService, atLeastOnce()).sendTemplateEmail(
            to = eq("verify@example.com"),
            subject = argThat { subject -> subject.contains("Verify") },
            templateName = eq(EmailTemplateNames.EMAIL_VERIFICATION),
            context = any()
        )
    }

    @Test
    fun `publishing UserApplicationEvent triggers both welcome and verification emails`() {
        // -- arrange --
        val mockUser = User("both@example.com", "password")
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "both-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        whenever(mockVerificationTokenService.generateToken(any())).thenReturn(mockToken)

        val userEventRequest = UserEventRequest(mockUser)
        val event = UserApplicationEvent(userEventRequest)

        // -- act --
        applicationEventPublisher.publishEvent(event)

        // -- give listeners time to process --
        Thread.sleep(100)

        // -- assert --
        // Verify welcome email was sent
        verify(mockEmailService, atLeastOnce()).sendTemplateEmail(
            to = eq("both@example.com"),
            subject = argThat { subject -> subject.contains("Welcome") },
            templateName = eq(EmailTemplateNames.WELCOME),
            context = any()
        )

        // Verify verification email was sent
        verify(mockEmailService, atLeastOnce()).sendTemplateEmail(
            to = eq("both@example.com"),
            subject = argThat { subject -> subject.contains("Verify") },
            templateName = eq(EmailTemplateNames.EMAIL_VERIFICATION),
            context = any()
        )

        // Verify total of 2 emails were sent
        verify(mockEmailService, times(2)).sendTemplateEmail(
            to = any(),
            subject = any(),
            templateName = any(),
            context = any()
        )
    }

    @Test
    fun `publishing EmailApplicationEvent sends email directly`() {
        // -- arrange --
        val emailEventRequest = EmailEventRequest(
            to = "direct@example.com",
            subject = "Direct Email Test",
            templateName = "welcome",
            context = mapOf("USER_NAME" to "Direct User")
        )
        val event = EmailApplicationEvent(emailEventRequest)

        // -- act --
        applicationEventPublisher.publishEvent(event)

        // -- give listener time to process --
        Thread.sleep(100)

        // -- assert --
        verify(mockEmailService).sendTemplateEmail(
            to = eq("direct@example.com"),
            subject = eq("Direct Email Test"),
            templateName = eq("welcome"),
            context = eq(mapOf("USER_NAME" to "Direct User"))
        )
    }

    @Test
    fun `multiple event publications are handled independently`() {
        // -- arrange --
        val user1 = User("user1@example.com", "password")
        val user2 = User("user2@example.com", "password")

        val token1 = EmailVerificationToken(
            user = user1,
            token = "user1-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        val token2 = EmailVerificationToken(
            user = user2,
            token = "user2-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        whenever(mockVerificationTokenService.generateToken(user1)).thenReturn(token1)
        whenever(mockVerificationTokenService.generateToken(user2)).thenReturn(token2)

        val event1 = UserApplicationEvent(UserEventRequest(user1))
        val event2 = UserApplicationEvent(UserEventRequest(user2))

        // -- act --
        applicationEventPublisher.publishEvent(event1)
        applicationEventPublisher.publishEvent(event2)

        // -- give listeners time to process --
        Thread.sleep(200)

        // -- assert --
        verify(mockEmailService, atLeastOnce()).sendTemplateEmail(
            to = eq("user1@example.com"),
            subject = any(),
            templateName = any(),
            context = any()
        )

        verify(mockEmailService, atLeastOnce()).sendTemplateEmail(
            to = eq("user2@example.com"),
            subject = any(),
            templateName = any(),
            context = any()
        )
    }

    @Test
    fun `email service failure in one listener does not affect others`() {
        // -- arrange --
        val mockUser = User("failure@example.com", "password")
        val mockToken = EmailVerificationToken(
            user = mockUser,
            token = "failure-token",
            expiresAt = OffsetDateTime.now().plusHours(24)
        )
        whenever(mockVerificationTokenService.generateToken(any())).thenReturn(mockToken)

        val userEventRequest = UserEventRequest(mockUser)
        val event = UserApplicationEvent(userEventRequest)

        // Mock welcome email to fail
        whenever(
            mockEmailService.sendTemplateEmail(
                to = any(),
                subject = argThat { subject -> subject.contains("Welcome") },
                templateName = eq(EmailTemplateNames.WELCOME),
                context = any()
            )
        ).thenThrow(RuntimeException("Welcome email failed"))

        // -- act --
        applicationEventPublisher.publishEvent(event)

        // -- give listeners time to process --
        Thread.sleep(100)

        // -- assert --
        // Verification email should still be sent despite welcome email failure
        verify(mockEmailService, atLeastOnce()).sendTemplateEmail(
            to = eq("failure@example.com"),
            subject = argThat { subject -> subject.contains("Verify") },
            templateName = eq(EmailTemplateNames.EMAIL_VERIFICATION),
            context = any()
        )
    }
}
