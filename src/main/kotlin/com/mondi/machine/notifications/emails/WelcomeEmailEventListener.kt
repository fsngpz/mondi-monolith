package com.mondi.machine.notifications.emails

import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * The event listener for sending welcome emails when a new user registers.
 *
 * This listener subscribes to UserApplicationEvent and sends a welcome email
 * to the newly registered user asynchronously using the email template service.
 *
 * @author Ferdinand Sangap.
 * @since 2026-02-09
 */
@Component
class WelcomeEmailEventListener(
    private val emailService: EmailService,
    @Value("\${app.url.collections}") private val collectionsUrl: String,
    @Value("\${app.url.instagram}") private val instagramUrl: String,
    @Value("\${app.url.tiktok}") private val tiktokUrl: String
) : ApplicationListener<UserApplicationEvent> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * an override function to handle the [ApplicationListener] of [UserApplicationEvent].
     *
     * Sends a welcome email to the newly registered user with personalized content
     * and links to collections and social media. This method runs asynchronously
     * in a separate thread to avoid blocking the main request.
     *
     * @param event the [UserApplicationEvent] instance containing user information.
     */
    @Async
    override fun onApplicationEvent(event: UserApplicationEvent) {
        logger.info("Receiving user registration event for welcome email: $event")

        val payload = event.source as UserEventRequest
        val user = payload.user

        // -- extract user's name from profile or use email as fallback --
        val userName = user.profile?.name ?: user.email.substringBefore("@")

        // -- prepare email context --
        val context = mapOf(
            "USER_NAME" to userName,
            "COLLECTIONS_URL" to collectionsUrl,
            "INSTAGRAM_URL" to instagramUrl,
            "FACEBOOK_URL" to tiktokUrl // Note: template uses FACEBOOK_URL for TikTok
        )

        try {
            // -- send welcome email --
            emailService.sendTemplateEmail(
                to = user.email,
                subject = "Welcome to Mondi Jewellery - Discover Elegant Pieces",
                templateName = EmailTemplateNames.WELCOME,
                context = context
            )
            logger.info("Welcome email sent successfully to: ${user.email}")
        } catch (e: Exception) {
            logger.error("Failed to send welcome email to: ${user.email}", e)
            // Note: We don't re-throw the exception to avoid blocking other listeners
        }
    }
}
