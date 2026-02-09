package com.mondi.machine.notifications.emails

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * The event listener for email notifications.
 *
 * @author Ferdinand Sangap.
 * @since 2026-02-09
 */
@Component
class EmailEventListener(
    private val emailService: EmailService
) : ApplicationListener<EmailApplicationEvent> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * an override function to handle the [ApplicationListener] of [EmailApplicationEvent].
     *
     * @param event the [EmailApplicationEvent] instance.
     */
    override fun onApplicationEvent(event: EmailApplicationEvent) {
        logger.info("Receiving the email event with value: $event")
        val payload = event.source as EmailEventRequest

        try {
            // -- send email --
            emailService.sendTemplateEmail(
                to = payload.to,
                subject = payload.subject,
                templateName = payload.templateName,
                context = payload.context
            )
            logger.info("Finished handle email event with value: $event")
        } catch (e: Exception) {
            logger.error("Failed to send email to: ${payload.to}", e)
            // Note: We don't re-throw the exception to avoid blocking other listeners
        }
    }
}
