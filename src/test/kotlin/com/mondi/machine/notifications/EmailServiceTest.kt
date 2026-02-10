package com.mondi.machine.notifications

import com.mondi.machine.exceptions.EmailSendException
import com.mondi.machine.notifications.emails.EmailService
import jakarta.mail.internet.MimeMessage
import org.apache.velocity.app.VelocityEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Year

/**
 * The test class for [com.mondi.machine.notifications.emails.EmailService].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-31
 */
@SpringBootTest(classes = [EmailService::class, com.mondi.machine.configs.EmailConfig::class])
@TestPropertySource(
    properties = [
        "mail.from.email=test@mondi.com",
        "mail.from.name=Mondi Test"
    ]
)
internal class EmailServiceTest(
    @Autowired private val service: EmailService,
    @Autowired private val velocityEngine: VelocityEngine
) {

    @MockitoBean
    lateinit var mockMailSender: JavaMailSender

    // -- region: Smoke Tests --

    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
        assertThat(mockMailSender).isNotNull
        assertThat(velocityEngine).isNotNull
    }

    // -- end of region: Smoke Tests --

    // -- region: Simple Email Tests --

    @Test
    fun `sendSimpleEmail sends email successfully`() {
        // -- prepare --
        val to = "recipient@example.com"
        val subject = "Test Subject"
        val text = "Test email body"

        // -- execute --
        service.sendSimpleEmail(to, subject, text)

        // -- verify --
        val messageCaptor = argumentCaptor<SimpleMailMessage>()
        verify(mockMailSender).send(messageCaptor.capture())

        val capturedMessage = messageCaptor.firstValue
        assertThat(capturedMessage.to).contains(to)
        assertThat(capturedMessage.subject).isEqualTo(subject)
        assertThat(capturedMessage.text).isEqualTo(text)
        assertThat(capturedMessage.from).contains("test@mondi.com")
    }

    @Test
    fun `sendSimpleEmail throws EmailSendException on failure`() {
        // -- prepare --
        val to = "recipient@example.com"
        val subject = "Test Subject"
        val text = "Test email body"

        whenever(mockMailSender.send(any<SimpleMailMessage>())).thenThrow(RuntimeException("SMTP error"))

        // -- execute and verify --
        val exception = assertThrows<EmailSendException> {
            service.sendSimpleEmail(to, subject, text)
        }

        assertThat(exception.message).contains("Failed to send email to $to")
        assertThat(exception.cause).isInstanceOf(RuntimeException::class.java)
    }

    // -- end of region: Simple Email Tests --

    // -- region: HTML Email Tests --

    @Test
    fun `sendHtmlEmail sends HTML email successfully`() {
        // -- prepare --
        val to = "recipient@example.com"
        val subject = "HTML Email"
        val htmlContent = "<html><body><h1>Hello</h1></body></html>"
        val mockMimeMessage = org.mockito.kotlin.mock<MimeMessage>()

        whenever(mockMailSender.createMimeMessage()).thenReturn(mockMimeMessage)

        // -- execute --
        service.sendHtmlEmail(to, subject, htmlContent)

        // -- verify --
        verify(mockMailSender).createMimeMessage()
        verify(mockMailSender).send(mockMimeMessage)
    }

    @Test
    fun `sendHtmlEmail throws EmailSendException on failure`() {
        // -- prepare --
        val to = "recipient@example.com"
        val subject = "HTML Email"
        val htmlContent = "<html><body><h1>Hello</h1></body></html>"

        whenever(mockMailSender.createMimeMessage()).thenThrow(RuntimeException("SMTP error"))

        // -- execute and verify --
        val exception = assertThrows<EmailSendException> {
            service.sendHtmlEmail(to, subject, htmlContent)
        }

        assertThat(exception.message).contains("Failed to send HTML email to $to")
    }

    // -- end of region: HTML Email Tests --

    // -- region: Template Email Tests --

    @Test
    fun `sendTemplateEmail sends email with welcome template successfully`() {
        // -- prepare --
        val to = "newuser@example.com"
        val subject = "Welcome to Mondi"
        val context = mapOf(
            "name" to "John Doe",
            "verificationUrl" to "https://mondi.com/verify/123",
            "year" to Year.now().value.toString()
        )
        val mockMimeMessage = org.mockito.kotlin.mock<MimeMessage>()

        whenever(mockMailSender.createMimeMessage()).thenReturn(mockMimeMessage)

        // -- execute --
        service.sendTemplateEmail(to, subject, "welcome", context)

        // -- verify --
        verify(mockMailSender).createMimeMessage()
        verify(mockMailSender).send(mockMimeMessage)
    }

    @Test
    fun `sendTemplateEmail sends email with password-reset template successfully`() {
        // -- prepare --
        val to = "user@example.com"
        val subject = "Password Reset Request"
        val context = mapOf(
            "name" to "Jane Doe",
            "resetUrl" to "https://mondi.com/reset/abc123",
            "resetCode" to "123456",
            "expirationMinutes" to "30",
            "year" to Year.now().value.toString()
        )
        val mockMimeMessage = org.mockito.kotlin.mock<MimeMessage>()

        whenever(mockMailSender.createMimeMessage()).thenReturn(mockMimeMessage)

        // -- execute --
        service.sendTemplateEmail(to, subject, "password-reset", context)

        // -- verify --
        verify(mockMailSender).createMimeMessage()
        verify(mockMailSender).send(mockMimeMessage)
    }

    @Disabled("Disabled until the order-confirmation template is implemented")
    @Test
    fun `sendTemplateEmail sends email with order-confirmation template successfully`() {
        // -- prepare --
        val to = "customer@example.com"
        val subject = "Order Confirmation"
        val items = listOf(
            mapOf("name" to "Product A", "quantity" to 2, "price" to "29.99"),
            mapOf("name" to "Product B", "quantity" to 1, "price" to "49.99")
        )
        val context = mapOf(
            "customerName" to "Bob Smith",
            "orderNumber" to "ORD-123456",
            "orderDate" to "2026-01-31",
            "estimatedDelivery" to "2026-02-05",
            "items" to items,
            "totalAmount" to "109.97",
            "trackingUrl" to "https://mondi.com/track/ORD-123456",
            "year" to Year.now().value.toString()
        )
        val mockMimeMessage = org.mockito.kotlin.mock<MimeMessage>()

        whenever(mockMailSender.createMimeMessage()).thenReturn(mockMimeMessage)

        // -- execute --
        service.sendTemplateEmail(to, subject, "order-confirmation", context)

        // -- verify --
        verify(mockMailSender).createMimeMessage()
        verify(mockMailSender).send(mockMimeMessage)
    }

    @Test
    fun `sendTemplateEmail throws EmailSendException when template not found`() {
        // -- prepare --
        val to = "recipient@example.com"
        val subject = "Test"
        val context = mapOf("name" to "Test User")

        // -- execute and verify --
        val exception = assertThrows<EmailSendException> {
            service.sendTemplateEmail(to, subject, "non-existent-template", context)
        }

        assertThat(exception.message).contains("Failed to send template email to $to")
    }

    // -- end of region: Template Email Tests --

    // -- region: Email with Attachment Tests --

    @Test
    fun `sendTemplateEmailWithAttachment sends email with attachment successfully`() {
        // -- prepare --
        val to = "recipient@example.com"
        val subject = "Email with Attachment"
        val context = mapOf(
            "name" to "John Doe",
            "year" to Year.now().value.toString()
        )
        val attachmentName = "invoice.pdf"
        val attachmentData = "PDF content".toByteArray()
        val mockMimeMessage = org.mockito.kotlin.mock<MimeMessage>()

        whenever(mockMailSender.createMimeMessage()).thenReturn(mockMimeMessage)

        // -- execute --
        service.sendTemplateEmailWithAttachment(
            to,
            subject,
            "welcome",
            context,
            attachmentName,
            attachmentData
        )

        // -- verify --
        verify(mockMailSender).createMimeMessage()
        verify(mockMailSender).send(mockMimeMessage)
    }

    @Test
    fun `sendTemplateEmailWithAttachment throws EmailSendException on failure`() {
        // -- prepare --
        val to = "recipient@example.com"
        val subject = "Email with Attachment"
        val context = mapOf("name" to "Test User")
        val attachmentName = "file.txt"
        val attachmentData = "content".toByteArray()

        whenever(mockMailSender.createMimeMessage()).thenThrow(RuntimeException("Failed to create message"))

        // -- execute and verify --
        val exception = assertThrows<EmailSendException> {
            service.sendTemplateEmailWithAttachment(
                to,
                subject,
                "welcome",
                context,
                attachmentName,
                attachmentData
            )
        }

        assertThat(exception.message).contains("Failed to send email with attachment to $to")
    }

    // -- end of region: Email with Attachment Tests --
}
