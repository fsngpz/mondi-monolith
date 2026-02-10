package com.mondi.machine.notifications.emails

import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import com.mondi.machine.auths.verification.EmailVerificationTokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * The event listener for sending email verification emails when a new user registers.
 *
 * This listener subscribes to UserApplicationEvent and sends an email verification
 * link to the newly registered user. The verification URL includes a secure token
 * with expiration that is stored in the database.
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
     * Sends an email verification link to the newly registered user.
     *
     * @param event the [UserApplicationEvent] instance containing user information.
     */
    override fun onApplicationEvent(event: UserApplicationEvent) {
        logger.info("Receiving user registration event for email verification: $event")

        val payload = event.source as UserEventRequest
        val user = payload.user

        // Skip if email is already verified (e.g., OAuth users)
        if (user.isEmailVerified) {
            logger.info("User ${user.email} already verified, skipping verification email")
            return
        }

        // -- extract user's name from profile or use email as fallback --
        val userName = user.profile?.name ?: user.email.substringBefore("@")

        // -- generate secure verification token with expiration --
        val verificationToken = verificationTokenService.generateToken(user)
        val verificationUrl = "$baseUrl/v1/auth/verify-email?token=${verificationToken.token}"

        // -- prepare email context --
        val context = mapOf(
            "USER_NAME" to userName,
            "VERIFICATION_URL" to verificationUrl
        )

        try {
            // -- send verification email --
            emailService.sendTemplateEmail(
                to = user.email,
                subject = "Verify Your Email - Mondi Jewellery",
                templateName = EmailTemplateNames.EMAIL_VERIFICATION,
                context = context
            )
            logger.info("Email verification sent successfully to: ${user.email}")
        } catch (e: Exception) {
            logger.error("Failed to send email verification to: ${user.email}", e)
            // Note: We don't re-throw the exception to avoid blocking other listeners
        }
    }
}
