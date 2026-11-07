package com.mondi.machine.backoffices.orders

import com.mondi.machine.exceptions.InvalidOrderStatusTransitionException
import com.mondi.machine.orders.Order
import com.mondi.machine.orders.OrderRepository
import com.mondi.machine.orders.OrderStatus
import com.mondi.machine.orders.OrderTestFixtures
import com.mondi.machine.orders.OrderTestFixtures.withItem
import com.mondi.machine.orders.ShippingCourier
import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductRepository
import com.mondi.machine.storage.supabase.SupabaseService
import com.mondi.machine.storage.supabase.SupabaseService.Companion.BUCKET_CERTIFICATES
import java.util.Optional
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus

/**
 * The test class for [BackofficeOrderService].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@SpringBootTest(classes = [BackofficeOrderService::class])
internal class BackofficeOrderServiceTest(@Autowired private val service: BackofficeOrderService) {
    // -- region of mock --
    @MockitoBean
    lateinit var mockOrderRepository: OrderRepository

    @MockitoBean
    lateinit var mockProductRepository: ProductRepository

    @MockitoBean
    lateinit var mockSupabaseService: SupabaseService

    @MockitoBean
    lateinit var mockTransactionManager: PlatformTransactionManager
    // -- end of region mock --

    @BeforeEach
    fun setUpTransactionManager() {
        // -- the service runs part of its work in a TransactionTemplate --
        whenever(mockTransactionManager.getTransaction(any())).thenReturn(SimpleTransactionStatus())
    }

    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
        assertThat(mockOrderRepository).isNotNull
    }

    // -- region of status transitions --
    @ParameterizedTest
    @CsvSource(
        "PENDING_PAYMENT, PAYMENT_CONFIRMED",
        "PAYMENT_CONFIRMED, PROCESSING",
        "PROCESSING, SHIPPED",
        "SHIPPED, DELIVERED",
        "PENDING_PAYMENT, CANCELLED",
        "PAYMENT_CONFIRMED, CANCELLED",
        "PROCESSING, CANCELLED"
    )
    fun `update status with an allowed transition`(from: OrderStatus, to: OrderStatus) {
        val order = OrderTestFixtures.order(status = from)
        mockFindAndSave(order)
        // -- shipping needs a courier --
        val courier = if (to == OrderStatus.SHIPPED) ShippingCourier.JNE else null
        val trackingNumber = if (to == OrderStatus.SHIPPED) "CGK1234567890" else null

        // -- execute --
        val result = service.updateStatus(10L, to, courier, trackingNumber)

        // -- verify --
        assertThat(result.status).isEqualTo(to)
        verify(mockOrderRepository).save(order)
    }

    @ParameterizedTest
    @CsvSource(
        "DELIVERED, PENDING_PAYMENT",
        "DELIVERED, CANCELLED",
        "CANCELLED, PENDING_PAYMENT",
        "SHIPPED, CANCELLED",
        "PENDING_PAYMENT, SHIPPED",
        "PAYMENT_CONFIRMED, PAYMENT_CONFIRMED"
    )
    fun `update status with a transition that is not allowed`(from: OrderStatus, to: OrderStatus) {
        val order = OrderTestFixtures.order(status = from)
        mockFindAndSave(order)

        // -- execute --
        assertThrows<InvalidOrderStatusTransitionException> { service.updateStatus(10L, to) }

        // -- verify --
        assertThat(order.status).isEqualTo(from)
        verify(mockOrderRepository, never()).save(any<Order>())
    }

    @Test
    fun `cancelling an order puts the items back in stock`() {
        val product = OrderTestFixtures.product(stock = 2)
        val order = OrderTestFixtures.order(status = OrderStatus.PAYMENT_CONFIRMED)
            .withItem(product, quantity = 3)
            // -- an item whose product was deleted is skipped --
            .withItem(null, quantity = 1, itemId = 101L)
        mockFindAndSave(order)

        // -- execute --
        service.updateStatus(10L, OrderStatus.CANCELLED)

        // -- verify --
        assertThat(product.stock).isEqualTo(5)
        verify(mockProductRepository).saveAll(listOf(product))
    }

    @Test
    fun `update status of an unknown order`() {
        whenever(mockOrderRepository.findById(10L)).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { service.updateStatus(10L, OrderStatus.CANCELLED) }
    }
    // -- end of region status transitions --

    // -- region of shipment --
    @Test
    fun `marking as shipped without a shipment is rejected`() {
        val order = OrderTestFixtures.order(status = OrderStatus.PROCESSING)
        mockFindAndSave(order)

        // -- execute --
        val exception = assertThrows<IllegalArgumentException> { service.updateStatus(10L, OrderStatus.SHIPPED) }

        // -- verify --
        assertThat(exception.message).contains("courier")
        assertThat(order.status).isEqualTo(OrderStatus.PROCESSING)
        verify(mockOrderRepository, never()).save(any<Order>())
    }

    @Test
    fun `marking as shipped with the shipment in the request sets it and the shipped date`() {
        val order = OrderTestFixtures.order(status = OrderStatus.PROCESSING)
        mockFindAndSave(order)

        // -- execute --
        val result = service.updateStatus(10L, OrderStatus.SHIPPED, ShippingCourier.JNT, "  JD0123456789 ")

        // -- verify --
        assertThat(result.shipment?.courier).isEqualTo(ShippingCourier.JNT)
        assertThat(result.shipment?.trackingNumber).isEqualTo("JD0123456789")
        assertThat(result.shipment?.shippedAt).isNotNull()
    }

    @Test
    fun `marking as shipped uses a shipment set earlier`() {
        val order = OrderTestFixtures.order(status = OrderStatus.PROCESSING).apply {
            shippingCourier = ShippingCourier.SICEPAT
            trackingNumber = "004512345678"
        }
        mockFindAndSave(order)

        // -- execute --
        val result = service.updateStatus(10L, OrderStatus.SHIPPED)

        // -- verify --
        assertThat(result.status).isEqualTo(OrderStatus.SHIPPED)
        assertThat(result.shipment?.trackingNumber).isEqualTo("004512345678")
    }

    @Test
    fun `in-house delivery does not need a tracking number`() {
        val order = OrderTestFixtures.order(status = OrderStatus.PROCESSING)
        mockFindAndSave(order)

        // -- execute --
        val result = service.updateStatus(10L, OrderStatus.SHIPPED, ShippingCourier.IN_HOUSE, null)

        // -- verify --
        assertThat(result.shipment?.courier).isEqualTo(ShippingCourier.IN_HOUSE)
        assertThat(result.shipment?.trackingNumber).isNull()
    }

    @Test
    fun `a courier service needs a tracking number`() {
        val order = OrderTestFixtures.order(status = OrderStatus.PAYMENT_CONFIRMED)
        mockFindAndSave(order)

        // -- execute & verify --
        assertThrows<IllegalArgumentException> { service.updateShipment(10L, ShippingCourier.JNE, "  ") }
        verify(mockOrderRepository, never()).save(any<Order>())
    }

    @Test
    fun `a tracking number with spaces or symbols is rejected`() {
        val order = OrderTestFixtures.order(status = OrderStatus.PAYMENT_CONFIRMED)
        mockFindAndSave(order)

        // -- execute & verify --
        assertThrows<IllegalArgumentException> { service.updateShipment(10L, ShippingCourier.JNE, "CGK 123/45") }
    }

    @ParameterizedTest
    @CsvSource("PAYMENT_CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED")
    fun `shipment can be set once payment is confirmed`(status: OrderStatus) {
        val order = OrderTestFixtures.order(status = status)
        mockFindAndSave(order)

        // -- execute --
        val result = service.updateShipment(10L, ShippingCourier.ANTERAJA, "10001234567890")

        // -- verify --
        assertThat(result.shipment?.courier).isEqualTo(ShippingCourier.ANTERAJA)
        assertThat(result.status).isEqualTo(status)
    }

    @ParameterizedTest
    @CsvSource("PENDING_PAYMENT", "CANCELLED")
    fun `shipment cannot be set before payment or on a cancelled order`(status: OrderStatus) {
        val order = OrderTestFixtures.order(status = status)
        mockFindAndSave(order)

        // -- execute & verify --
        assertThrows<IllegalArgumentException> {
            service.updateShipment(10L, ShippingCourier.JNE, "CGK1234567890")
        }
        verify(mockOrderRepository, never()).save(any<Order>())
    }
    // -- end of region shipment --

    // -- region of certificate upload --
    @Test
    fun `upload certificate for an order item`() = runTest {
        val order = OrderTestFixtures.order(status = OrderStatus.DELIVERED).withItem(OrderTestFixtures.product())
        val file = MockMultipartFile("file", "certificate.pdf", "application/pdf", ByteArray(16))
        mockFindAndSave(order)
        whenever(mockSupabaseService.uploadFile(any(), any(), any(), any()))
            .thenReturn("certificates/1/order-10-item-100.pdf")

        // -- execute --
        val result = service.uploadCertificate(10L, 100L, file)

        // -- verify --
        assertThat(result.items.single().certificateUrl).isEqualTo("certificates/1/order-10-item-100.pdf")
        verify(mockSupabaseService).uploadFile(eq(BUCKET_CERTIFICATES), any(), eq(file), eq(false))
    }

    @Test
    fun `upload certificate for an item that is not in the order`() = runTest {
        val order = OrderTestFixtures.order().withItem(OrderTestFixtures.product())
        val file = MockMultipartFile("file", "certificate.pdf", "application/pdf", ByteArray(16))
        mockFindAndSave(order)

        // -- execute --
        assertThrows<NoSuchElementException> { service.uploadCertificate(10L, 999L, file) }

        // -- verify --
        verify(mockSupabaseService, never()).uploadFile(any(), any(), any(), any())
    }

    @Test
    fun `upload certificate for a cancelled order`() = runTest {
        val order = OrderTestFixtures.order(status = OrderStatus.CANCELLED).withItem(OrderTestFixtures.product())
        val file = MockMultipartFile("file", "certificate.pdf", "application/pdf", ByteArray(16))
        mockFindAndSave(order)

        // -- execute --
        assertThrows<IllegalArgumentException> { service.uploadCertificate(10L, 100L, file) }

        // -- verify --
        verify(mockSupabaseService, never()).uploadFile(any(), any(), any(), any())
    }
    // -- end of region certificate upload --

    /**
     * a private function to mock finding and saving the [order].
     */
    private fun mockFindAndSave(order: Order) {
        whenever(mockOrderRepository.findById(10L)).thenReturn(Optional.of(order))
        whenever(mockOrderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }
        whenever(mockProductRepository.saveAll(any<List<Product>>())).thenAnswer { it.arguments[0] }
    }
}
