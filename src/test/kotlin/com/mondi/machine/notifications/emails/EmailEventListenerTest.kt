package com.mondi.machine.notifications.emails

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * The test class of [EmailEventListener].
 *
 * @author Ferdinand Sangap.
 * @since 2026-02-09
 */
@SpringBootTest(classes = [EmailEventListener::class])
internal class EmailEventListenerTest(
    @Autowired private val listener: EmailEventListener
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
    fun `onApplicationEvent sends email successfully`() {
        // -- arrange --
        val to = "test@example.com"
        val subject = "Test Subject"
        val templateName = "welcome"
        val context = mapOf("USER_NAME" to "John Doe")

        val emailEventRequest = EmailEventRequest(
            to = to,
            subject = subject,
            templateName = templateName,
            context = context
        )
        val event = EmailApplicationEvent(emailEventRequest)

        // -- act --
        listener.onApplicationEvent(event)

        // -- assert --
        verify(mockEmailService).sendTemplateEmail(
            to = eq(to),
            subject = eq(subject),
            templateName = eq(templateName),
            context = eq(context)
        )
    }

    @Test
    fun `onApplicationEvent with all context parameters`() {
        // -- arrange --
        val context = mapOf(
            "USER_NAME" to "Jane Smith",
            "VERIFICATION_URL" to "https://example.com/verify",
            "COLLECTIONS_URL" to "https://example.com/collections"
        )

        val emailEventRequest = EmailEventRequest(
            to = "jane@example.com",
            subject = "Welcome Email",
            templateName = EmailTemplateNames.WELCOME,
            context = context
        )
        val event = EmailApplicationEvent(emailEventRequest)

        // -- act --
        listener.onApplicationEvent(event)

        // -- assert --
        verify(mockEmailService).sendTemplateEmail(
            to = eq("jane@example.com"),
            subject = eq("Welcome Email"),
            templateName = eq(EmailTemplateNames.WELCOME),
            context = eq(context)
        )
    }

    @Test
    fun `onApplicationEvent handles email service exception gracefully`() {
        // -- arrange --
        val emailEventRequest = EmailEventRequest(
            to = "error@example.com",
            subject = "Test Subject",
            templateName = "welcome",
            context = mapOf("USER_NAME" to "Test User")
        )
        val event = EmailApplicationEvent(emailEventRequest)

        whenever(mockEmailService.sendTemplateEmail(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Email service failure"))

        // -- act & assert --
        // Should not throw exception - error is logged and swallowed
        listener.onApplicationEvent(event)

        verify(mockEmailService).sendTemplateEmail(any(), any(), any(), any())
    }
}
