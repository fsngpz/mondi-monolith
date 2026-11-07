package com.mondi.machine.notifications.emails

import com.mondi.machine.orders.OrderShippedEvent
import com.mondi.machine.orders.ShippingCourier
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.util.HtmlUtils

/**
 * Sends the "your order has shipped" email with the courier and tracking number.
 *
 * Runs after the status change is committed (so no email for a change that rolled back) and on a
 * separate thread (so a slow mail server does not hold up the backoffice).
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Component
class OrderShippedEmailListener(
    private val emailService: EmailService,
    @Value("\${app.url.base}") private val baseUrl: String,
    @Value("\${app.url.instagram}") private val instagramUrl: String,
    @Value("\${app.url.tiktok}") private val tiktokUrl: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * a function to send the shipped email for an [OrderShippedEvent].
     * A failure is logged and not rethrown: the order is already shipped.
     *
     * @param event the [OrderShippedEvent].
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderShipped(event: OrderShippedEvent) {
        try {
            emailService.sendTemplateEmail(
                to = event.email,
                subject = "Your Mondi order #${event.orderId} is on its way",
                templateName = EmailTemplateNames.ORDER_SHIPPED,
                context = buildContext(event)
            )
            logger.info("Order shipped email sent for order ${event.orderId}")
        } catch (e: Exception) {
            logger.error("Failed to send order shipped email for order ${event.orderId}", e)
        }
    }

    /**
     * a function to build the template variables. Customer-provided text is HTML-escaped.
     *
     * @param event the [OrderShippedEvent].
     * @return the template context.
     */
    fun buildContext(event: OrderShippedEvent): Map<String, Any> {
        val escape = { value: String -> HtmlUtils.htmlEscape(value) }
        val context = mutableMapOf<String, Any>(
            "USER_NAME" to escape(event.customerName?.takeIf { it.isNotBlank() } ?: "there"),
            "ORDER_ID" to event.orderId,
            "COURIER" to escape(event.courier.displayName),
            "IN_HOUSE" to (event.courier == ShippingCourier.IN_HOUSE),
            "ITEMS" to event.items.map { mapOf("name" to escape(it.name), "quantity" to it.quantity) },
            "ORDER_URL" to "${baseUrl.trimEnd('/')}/account/orders/${event.orderId}",
            "INSTAGRAM_URL" to instagramUrl,
            "FACEBOOK_URL" to tiktokUrl
        )
        // -- optional values are left out; the template checks them with #if --
        event.trackingNumber?.let { context["TRACKING_NUMBER"] = escape(it) }
        event.courier.websiteUrl?.let { context["COURIER_URL"] = it }
        return context
    }
}
