package com.mondi.machine.notifications.emails

import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import com.mondi.machine.auths.verification.EmailVerificationTokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * The event listener for sending email verification emails when a new user registers.
 *
 * This listener subscribes to UserApplicationEvent and sends an email verification
 * link to the newly registered user asynchronously. The verification URL includes
 * a secure token with expiration that is stored in the database.
 *
 * @author Ferdinand Sangap.
 * @since 2026-02-09
 */
@Component
class EmailVerificationEventListener(
    private val emailService: EmailService,
    private val verificationTokenService: EmailVerificationTokenService,
    @Value("\${app.url.base}") private val baseUrl: String
) : ApplicationListener<UserApplicationEvent> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * an override function to handle the [ApplicationListener] of [UserApplicationEvent].
     *
     * Sends an email verification link to the newly registered user asynchronously.
     * This method runs in a separate thread to avoid blocking the main request.
     *
     * @param event the [UserApplicationEvent] instance containing user information.
     */
    @Async
    override fun onApplicationEvent(event: UserApplicationEvent) {
        logger.info("Receiving user registration event for email verification: $event")

        val payload = event.source as UserEventRequest

        // Skip if email is already verified (e.g., OAuth users)
        if (payload.isEmailVerified) {
            logger.info("User ${payload.email} already verified, skipping verification email")
            return
        }

        // Skip if no verification token provided
        if (payload.verificationToken == null) {
            logger.warn("No verification token provided for user ${payload.email}, skipping verification email")
            return
        }

        // -- build verification URL --
        val verificationUrl = "$baseUrl/verify-email?token=${payload.verificationToken}"

        // -- prepare email context --
        val context = mapOf(
            "USER_NAME" to payload.userName,
            "VERIFICATION_URL" to verificationUrl
        )

        try {
            // -- send verification email --
            emailService.sendTemplateEmail(
                to = payload.email,
                subject = "Verify Your Email - Mondi Jewellery",
                templateName = EmailTemplateNames.EMAIL_VERIFICATION,
                context = context
            )
            logger.info("Email verification sent successfully to: ${payload.email}")
        } catch (e: Exception) {
            logger.error("Failed to send email verification to: ${payload.email}", e)
            // Note: We don't re-throw the exception to avoid blocking other listeners
        }
    }
}
