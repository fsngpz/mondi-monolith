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

/**
 * The test class of [WelcomeEmailEventListener].
 *
 * @author Ferdinand Sangap.
 * @since 2026-02-09
 */
@SpringBootTest(classes = [WelcomeEmailEventListener::class])
@TestPropertySource(
    properties = [
        "app.url.collections=https://test.com/collections",
        "app.url.instagram=https://instagram.com/test",
        "app.url.tiktok=https://tiktok.com/test"
    ]
)
internal class WelcomeEmailEventListenerTest(
    @Autowired private val listener: WelcomeEmailEventListener
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
    fun `onApplicationEvent sends welcome email with user profile name`() {
        // -- arrange --
        val mockUser = User("john@example.com", "password").apply {
            this.id = 1L
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
            subject = eq("Welcome to Mondi Jewellery - Discover Elegant Pieces"),
            templateName = eq(EmailTemplateNames.WELCOME),
            context = argThat { context ->
                context["USER_NAME"] == "John" &&
                        context["COLLECTIONS_URL"] == "https://test.com/collections" &&
                        context["INSTAGRAM_URL"] == "https://instagram.com/test" &&
                        context["FACEBOOK_URL"] == "https://tiktok.com/test"
            }
        )
    }

    @Test
    fun `onApplicationEvent sends welcome email with email username when profile is null`() {
        // -- arrange --
        val mockUser = User("jane.smith@example.com", "password").apply {
            this.id = 2L
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
            templateName = eq(EmailTemplateNames.WELCOME),
            context = argThat { context ->
                context["USER_NAME"] == "jane.smith"
            }
        )
    }

    @Test
    fun `onApplicationEvent sends welcome email with email username when name is null`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply {
            this.id = 3L
            this.profile = Profile(this).apply {
                this.name = null
            }
        }
        val mockRequest = UserEventRequest(mockUser)
        val mockEvent = UserApplicationEvent(mockRequest)

        // -- act --
        listener.onApplicationEvent(mockEvent)

        // -- assert --
        verify(mockEmailService).sendTemplateEmail(
            to = eq("test@example.com"),
            subject = any(),
            templateName = eq(EmailTemplateNames.WELCOME),
            context = argThat { context ->
                context["USER_NAME"] == "test"
            }
        )
    }

    @Test
    fun `onApplicationEvent handles email service exception gracefully`() {
        // -- arrange --
        val mockUser = User("error@example.com", "password").apply {
            this.id = 4L
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
            this.id = 5L
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
                        context.containsKey("COLLECTIONS_URL") &&
                        context.containsKey("INSTAGRAM_URL") &&
                        context.containsKey("FACEBOOK_URL") &&
                        context.size == 4
            }
        )
    }
}
