package com.mondi.machine.notifications.emails

import com.mondi.machine.exceptions.EmailSendException
import jakarta.mail.internet.MimeMessage
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.StringWriter

/**
 * The service class for sending emails with Velocity template support.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-31
 */
@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val velocityEngine: VelocityEngine,
    @Value("\${mail.from.email}") private val fromEmail: String,
    @Value("\${mail.from.name}") private val fromName: String
) {

    private val logger: Logger = LoggerFactory.getLogger(EmailService::class.java)

    /**
     * Send a simple text email.
     *
     * @param to the recipient email address.
     * @param subject the email subject.
     * @param text the email body text.
     */
    fun sendSimpleEmail(to: String, subject: String, text: String) {
        try {
            val message = SimpleMailMessage()
            message.from = "$fromName <$fromEmail>"
            message.setTo(to)
            message.subject = subject
            message.text = text

            mailSender.send(message)
            logger.info("Simple email sent successfully to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send simple email to: $to", e)
            throw EmailSendException("Failed to send email to $to: ${e.message}", e)
        }
    }

    /**
     * Send an HTML email using a Velocity template.
     *
     * @param to the recipient email address.
     * @param subject the email subject.
     * @param templateName the name of the Velocity template file (without .vm extension).
     * @param context a map of variables to be used in the template.
     */
    fun sendTemplateEmail(
        to: String,
        subject: String,
        templateName: String,
        context: Map<String, Any>
    ) {
        try {
            // -- render template with context --
            val htmlContent = renderTemplate(templateName, context)

            // -- send HTML email --
            sendHtmlEmail(to, subject, htmlContent)

            logger.info("Template email sent successfully to: $to using template: $templateName")
        } catch (e: Exception) {
            logger.error("Failed to send template email to: $to using template: $templateName", e)
            throw EmailSendException("Failed to send template email to $to: ${e.message}", e)
        }
    }

    /**
     * Send an HTML email.
     *
     * @param to the recipient email address.
     * @param subject the email subject.
     * @param htmlContent the HTML content of the email.
     */
    fun sendHtmlEmail(to: String, subject: String, htmlContent: String) {
        try {
            val mimeMessage: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            helper.setFrom("$fromName <$fromEmail>")
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(htmlContent, true) // -- true indicates HTML --

            mailSender.send(mimeMessage)
            logger.info("HTML email sent successfully to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send HTML email to: $to", e)
            throw EmailSendException("Failed to send HTML email to $to: ${e.message}", e)
        }
    }

    /**
     * a private helper function to render a Velocity template with the provided context.
     *
     * This method encapsulates the template rendering logic to follow DRY principle,
     * eliminating code duplication between sendTemplateEmail and sendTemplateEmailWithAttachment.
     *
     * @param templateName the name of the Velocity template file (without .vm extension).
     * @param context a map of variables to be used in the template.
     * @return the rendered HTML content as a string.
     * @throws Exception if template rendering fails.
     */
    private fun renderTemplate(templateName: String, context: Map<String, Any>): String {
        // -- create velocity context --
        val velocityContext = VelocityContext()
        context.forEach { (key, value) ->
            velocityContext.put(key, value)
        }
        velocityContext.put("SUPPORT_EMAIL", "mondijewellery@gmail.com")

        // -- merge template with context --
        val writer = StringWriter()
        val template = velocityEngine.getTemplate("templates/email/$templateName.vm")
        template.merge(velocityContext, writer)

        return writer.toString()
    }

    /**
     * Send an email with attachment using Velocity template.
     *
     * @param to the recipient email address.
     * @param subject the email subject.
     * @param templateName the name of the Velocity template file (without .vm extension).
     * @param context a map of variables to be used in the template.
     * @param attachmentName the name of the attachment file.
     * @param attachmentData the byte array of the attachment data.
     */
    fun sendTemplateEmailWithAttachment(
        to: String,
        subject: String,
        templateName: String,
        context: Map<String, Any>,
        attachmentName: String,
        attachmentData: ByteArray
    ) {
        try {
            // -- render template with context --
            val htmlContent = renderTemplate(templateName, context)

            // -- create mime message --
            val mimeMessage: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            helper.setFrom("$fromName <$fromEmail>")
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(htmlContent, true)

            // -- add attachment --
            helper.addAttachment(attachmentName, ByteArrayResource(attachmentData))

            mailSender.send(mimeMessage)
            logger.info("Template email with attachment sent successfully to: $to")
        } catch (e: Exception) {
            logger.error("Failed to send template email with attachment to: $to", e)
            throw EmailSendException("Failed to send email with attachment to $to: ${e.message}", e)
        }
    }
}
