package com.mondi.machine.notifications.emails

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * The test class for integration test. Usually this will be disabled in CI/CD pipelines.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-31
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Disabled // TODO: Always disable this test in CI/CD pipelines.
internal class EmailServiceIntegrationTest {
    @Autowired
    private lateinit var emailService: EmailService

    @Test
    fun `dependencies should not be null`() {
        assertNotNull(emailService)
    }

    @Test
    fun `send simple email`() {
        val context = mapOf(
            "name" to "Ferdinand Sangap",
            "verificationUrl" to "https://example.com/verify?token=abc123",
            "year" to "2026"
        )
        emailService.sendTemplateEmail(
            to = "fsangap18@gmail.com",
            subject = "Test Simple Email",
            templateName = "welcome",
            context = context
        )
    }
}
