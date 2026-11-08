package com.mondi.machine.notifications.emails

import com.mondi.machine.configs.EmailConfig
import com.mondi.machine.exceptions.EmailSendException
import com.mondi.machine.orders.OrderShippedEvent
import com.mondi.machine.orders.ShippingCourier
import java.io.StringWriter
import org.apache.velocity.VelocityContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The test class for [OrderShippedEmailListener] and the `order-shipped` template.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
internal class OrderShippedEmailListenerTest {

    private val emailService: EmailService = mock()
    private val listener = OrderShippedEmailListener(
        emailService,
        baseUrl = "https://mondijewellery.studio/",
        instagramUrl = "https://instagram.com/mondijewellery",
        tiktokUrl = "https://tiktok.com/@mondijewellery"
    )

    private fun event(
        courier: ShippingCourier = ShippingCourier.JNE,
        trackingNumber: String? = "CGK1234567890",
        name: String? = "Jane <b>Doe</b>"
    ) = OrderShippedEvent(
        orderId = 42L,
        email = "jane@example.com",
        customerName = name,
        courier = courier,
        trackingNumber = trackingNumber,
        items = listOf(OrderShippedEvent.Item("Solitaire Ring", 1), OrderShippedEvent.Item("Pearl & Gold Earrings", 2))
    )

    private fun render(event: OrderShippedEvent): String {
        val engine = EmailConfig().velocityEngine()
        val context = VelocityContext().apply { listener.buildContext(event).forEach { (k, v) -> put(k, v) } }
        return StringWriter().also { engine.getTemplate("templates/email/order-shipped.vm").merge(context, it) }.toString()
    }

    @Test
    fun `sends the shipped email to the customer`() {
        listener.onOrderShipped(event())

        val context = argumentCaptor<Map<String, Any>>()
        verify(emailService).sendTemplateEmail(
            eq("jane@example.com"),
            eq("Your Mondi order #42 is on its way"),
            eq(EmailTemplateNames.ORDER_SHIPPED),
            context.capture()
        )
        assertThat(context.firstValue["ORDER_URL"]).isEqualTo("https://mondijewellery.studio/account/orders/42")
    }

    @Test
    fun `a mail failure is logged, not thrown`() {
        whenever(emailService.sendTemplateEmail(any(), any(), any(), any())).thenThrow(EmailSendException("smtp down", RuntimeException()))

        assertDoesNotThrow { listener.onOrderShipped(event()) }
    }

    @Test
    fun `template shows courier, tracking number, items and escapes customer text`() {
        val html = render(event())

        assertThat(html).contains("order <strong>#42</strong> has been handed to JNE")
        assertThat(html).contains("CGK1234567890")
        assertThat(html).contains("Tracking number")
        assertThat(html).contains("Solitaire Ring &times; 1")
        assertThat(html).contains("Pearl &amp; Gold Earrings &times; 2")
        assertThat(html).contains("https://www.jne.co.id")
        assertThat(html).contains("https://mondijewellery.studio/account/orders/42")
        // -- the customer's name cannot inject HTML --
        assertThat(html).contains("Jane &lt;b&gt;Doe&lt;/b&gt;")
        assertThat(html).doesNotContain("<b>Doe</b>")
        // -- every variable was filled --
        assertThat(html).doesNotContain("\${")
    }

    @Test
    fun `in-house delivery without a reference has no tracking row or courier link`() {
        val html = render(event(courier = ShippingCourier.IN_HOUSE, trackingNumber = null, name = null))

        assertThat(html).contains("Our own team is delivering it")
        assertThat(html).contains("Hi <strong>there</strong>")
        assertThat(html).doesNotContain("Tracking number")
        assertThat(html).doesNotContain("Track on")
        assertThat(html).doesNotContain("\${")
    }
}
