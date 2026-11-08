package com.mondi.machine.orders

import com.mondi.machine.orders.OrderTestFixtures.withItem
import com.mondi.machine.transactions.Transaction
import com.mondi.machine.transactions.TransactionRepository
import java.math.BigDecimal
import java.time.OffsetDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * The test class for [CertificateService].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@SpringBootTest(classes = [CertificateService::class])
internal class CertificateServiceTest(@Autowired private val service: CertificateService) {

    @MockitoBean
    lateinit var mockOrderItemRepository: OrderItemRepository

    @MockitoBean
    lateinit var mockTransactionRepository: TransactionRepository

    @Test
    fun `find all by user lists order certificates then legacy certificates`() {
        val profile = OrderTestFixtures.profile()
        val order = OrderTestFixtures.order(profile, OrderStatus.DELIVERED).withItem(OrderTestFixtures.product())
        val item = order.items.single().apply { certificateUrl = "certificates/1/order-10-item-100.pdf" }
        val legacy = Transaction(
            productName = "Legacy Ring",
            price = BigDecimal("100"),
            certificateUrl = "certificates/1/legacy.pdf",
            purchasedAt = OffsetDateTime.parse("2024-01-01T00:00:00Z"),
            profile = profile
        )
        whenever(mockOrderItemRepository.findAllWithCertificateByProfileId(1L, OrderStatus.CANCELLED)).thenReturn(listOf(item))
        whenever(mockTransactionRepository.findAllByProfileId(eq(1L), any())).thenReturn(listOf(legacy))

        // -- execute --
        val result = service.findAllByUser(1L)

        // -- verify --
        assertThat(result).hasSize(2)
        assertThat(result[0].source).isEqualTo(CertificateSource.ORDER)
        assertThat(result[0].orderId).isEqualTo(10L)
        assertThat(result[0].orderItemId).isEqualTo(100L)
        assertThat(result[0].certificateUrl).isEqualTo("certificates/1/order-10-item-100.pdf")
        assertThat(result[1].source).isEqualTo(CertificateSource.LEGACY_TRANSACTION)
        assertThat(result[1].orderId).isNull()
        assertThat(result[1].productName).isEqualTo("Legacy Ring")
    }

    @Test
    fun `find all by user without certificates`() {
        whenever(mockOrderItemRepository.findAllWithCertificateByProfileId(1L, OrderStatus.CANCELLED)).thenReturn(emptyList())
        whenever(mockTransactionRepository.findAllByProfileId(eq(1L), any())).thenReturn(emptyList())

        // -- execute & verify --
        assertThat(service.findAllByUser(1L)).isEmpty()
    }

    @Test
    fun `find all for the backoffice maps the customer`() {
        val order = OrderTestFixtures.order(status = OrderStatus.SHIPPED).withItem(OrderTestFixtures.product())
        val item = order.items.single().apply { certificateUrl = "certificates/key.pdf" }
        val pageable = PageRequest.of(0, 10)
        whenever(mockOrderItemRepository.findAllWithCertificate("ring", pageable))
            .thenReturn(PageImpl(listOf(item), pageable, 1))

        // -- execute --
        val result = service.findAll("  ring ", pageable)

        // -- verify --
        val certificate = result.content.single()
        assertThat(certificate.orderStatus).isEqualTo(OrderStatus.SHIPPED)
        assertThat(certificate.customer?.email).isEqualTo("customer@example.com")
    }
}
