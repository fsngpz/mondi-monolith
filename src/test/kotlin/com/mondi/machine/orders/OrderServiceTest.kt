package com.mondi.machine.orders

import com.mondi.machine.accounts.addresses.AddressService
import com.mondi.machine.accounts.profiles.ProfileService
import com.mondi.machine.exceptions.InsufficientStockException
import com.mondi.machine.orders.OrderTestFixtures.withItem
import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductRepository
import com.mondi.machine.products.ProductStatus
import com.mondi.machine.storage.supabase.SupabaseService
import com.mondi.machine.storage.supabase.SupabaseService.Companion.BUCKET_PAYMENT_PROOFS
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus

/**
 * The test class for [OrderService].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@SpringBootTest(classes = [OrderService::class])
internal class OrderServiceTest(@Autowired private val service: OrderService) {
    // -- region of mock --
    @MockitoBean
    lateinit var mockOrderRepository: OrderRepository

    @MockitoBean
    lateinit var mockProductRepository: ProductRepository

    @MockitoBean
    lateinit var mockAddressService: AddressService

    @MockitoBean
    lateinit var mockProfileService: ProfileService

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

    private val userId = 1L

    // -- region of smoke testing --
    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
        assertThat(mockOrderRepository).isNotNull
        assertThat(mockProductRepository).isNotNull
    }
    // -- end of region smoke testing --

    // -- region of create --
    @Test
    fun `create order snapshots prices and address and takes items off stock`() {
        val user = OrderTestFixtures.user(userId)
        val discounted = OrderTestFixtures.product(
            id = 1L, price = BigDecimal("1000000"), discountPrice = BigDecimal("800000"), stock = 5
        )
        val regular = OrderTestFixtures.product(id = 2L, price = BigDecimal("500000"), stock = 3)
        mockCreateDependencies(user, listOf(discounted, regular))
        val request = CreateOrderRequest(
            addressId = 5L,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            notes = "  please gift wrap  ",
            items = listOf(
                CreateOrderItemRequest(productId = 1L, quantity = 1),
                CreateOrderItemRequest(productId = 2L, quantity = 2),
                // -- the same product twice is merged into one line --
                CreateOrderItemRequest(productId = 1L, quantity = 1)
            )
        )

        // -- execute --
        val result = service.create(userId, request)

        // -- verify the amounts: 2 x 800,000 + 2 x 500,000 --
        assertThat(result.subtotal).isEqualByComparingTo("2600000")
        assertThat(result.discountAmount).isEqualByComparingTo("0")
        assertThat(result.totalPrice).isEqualByComparingTo("2600000")
        assertThat(result.status).isEqualTo(OrderStatus.PENDING_PAYMENT)
        assertThat(result.notes).isEqualTo("please gift wrap")
        // -- verify the items --
        assertThat(result.items).hasSize(2)
        assertThat(result.items[0].productId).isEqualTo(1L)
        assertThat(result.items[0].quantity).isEqualTo(2)
        assertThat(result.items[0].price).isEqualByComparingTo("800000")
        assertThat(result.items[1].price).isEqualByComparingTo("500000")
        // -- verify the address snapshot --
        assertThat(result.shippingAddress.recipientName).isEqualTo("Jane Doe")
        assertThat(result.shippingAddress.city).isEqualTo("Jakarta")
        // -- verify the stock was taken --
        assertThat(discounted.stock).isEqualTo(3)
        assertThat(regular.stock).isEqualTo(1)
        verify(mockProductRepository).saveAll(listOf(discounted, regular))
        verify(mockOrderRepository).save(any<Order>())
    }

    @Test
    fun `create order with an unknown product`() {
        val user = OrderTestFixtures.user(userId)
        mockCreateDependencies(user, emptyList())

        // -- execute --
        assertThrows<NoSuchElementException> { service.create(userId, createRequest(productId = 99L)) }

        // -- verify --
        verify(mockOrderRepository, never()).save(any<Order>())
    }

    @Test
    fun `create order with an inactive product`() {
        val user = OrderTestFixtures.user(userId)
        val product = OrderTestFixtures.product(status = ProductStatus.INACTIVE)
        mockCreateDependencies(user, listOf(product))

        // -- execute --
        val exception = assertThrows<IllegalArgumentException> { service.create(userId, createRequest()) }

        // -- verify --
        assertThat(exception.message).contains("no longer available")
        verify(mockOrderRepository, never()).save(any<Order>())
    }

    @Test
    fun `create order with more than the stock`() {
        val user = OrderTestFixtures.user(userId)
        val product = OrderTestFixtures.product(stock = 1)
        mockCreateDependencies(user, listOf(product))

        // -- execute --
        val exception = assertThrows<InsufficientStockException> {
            service.create(userId, createRequest(quantity = 2))
        }

        // -- verify the stock is untouched --
        assertThat(exception.message).contains("only 1 left")
        assertThat(product.stock).isEqualTo(1)
        verify(mockProductRepository, never()).saveAll(any<List<Product>>())
        verify(mockOrderRepository, never()).save(any<Order>())
    }

    @Test
    fun `create order with an address of another user`() {
        whenever(mockProfileService.get(userId)).thenReturn(OrderTestFixtures.profile())
        whenever(mockAddressService.getById(5L, userId))
            .thenThrow(IllegalArgumentException("Address does not belong to user"))

        // -- execute --
        assertThrows<IllegalArgumentException> { service.create(userId, createRequest()) }

        // -- verify --
        verify(mockProductRepository, never()).findAllByIdInForUpdate(any())
        verify(mockOrderRepository, never()).save(any<Order>())
    }

    @Test
    fun `create order without items`() {
        // -- execute --
        assertThrows<IllegalArgumentException> {
            service.create(userId, createRequest().copy(items = emptyList()))
        }

        // -- verify --
        verify(mockProfileService, never()).get(any())
    }

    @Test
    fun `create order with a zero quantity`() {
        // -- execute --
        assertThrows<IllegalArgumentException> { service.create(userId, createRequest(quantity = 0)) }
    }

    @Test
    fun `create order with a voucher code is rejected until vouchers exist`() {
        val user = OrderTestFixtures.user(userId)
        mockCreateDependencies(user, listOf(OrderTestFixtures.product()))

        // -- execute --
        assertThrows<IllegalArgumentException> {
            service.create(userId, createRequest().copy(voucherCode = "WELCOME10"))
        }

        // -- verify --
        verify(mockOrderRepository, never()).save(any<Order>())
    }
    // -- end of region create --

    // -- region of get --
    @Test
    fun `get order found`() {
        val order = OrderTestFixtures.order().withItem(OrderTestFixtures.product())
        whenever(mockOrderRepository.findByIdAndProfileId(10L, userId)).thenReturn(order)

        // -- execute --
        val result = service.get(10L, userId)

        // -- verify --
        assertThat(result.id).isEqualTo(10L)
        assertThat(result.items).hasSize(1)
        assertThat(result.customer?.email).isEqualTo("customer@example.com")
    }

    @Test
    fun `get order not found`() {
        whenever(mockOrderRepository.findByIdAndProfileId(10L, userId)).thenReturn(null)

        // -- execute --
        assertThrows<NoSuchElementException> { service.get(10L, userId) }
    }

    @Test
    fun `get order of another user is not found`() {
        // -- the repository only matches orders of the given profile --
        whenever(mockOrderRepository.findByIdAndProfileId(10L, 2L)).thenReturn(null)

        // -- execute --
        assertThrows<NoSuchElementException> { service.get(10L, 2L) }

        // -- verify --
        verify(mockOrderRepository).findByIdAndProfileId(10L, 2L)
    }
    // -- end of region get --

    // -- region of find all --
    @Test
    fun `find all orders paginated`() {
        val pageable = PageRequest.of(0, 2)
        val orders = listOf(OrderTestFixtures.order(id = 11L), OrderTestFixtures.order(id = 12L))
        whenever(mockOrderRepository.findAllByProfileId(userId, OrderStatus.PENDING_PAYMENT, pageable))
            .thenReturn(PageImpl(orders, pageable, 5))

        // -- execute --
        val result = service.findAll(userId, OrderStatus.PENDING_PAYMENT, pageable)

        // -- verify --
        assertThat(result.content.map { it.id }).containsExactly(11L, 12L)
        assertThat(result.totalElements).isEqualTo(5)
        assertThat(result.totalPages).isEqualTo(3)
    }
    // -- end of region find all --

    // -- region of payment proof --
    @Test
    fun `upload payment proof stores the file key`() = runTest {
        val order = OrderTestFixtures.order()
        val file = MockMultipartFile("file", "receipt.PNG", "image/png", ByteArray(16))
        whenever(mockOrderRepository.findByIdAndProfileId(10L, userId)).thenReturn(order)
        whenever(mockSupabaseService.uploadFile(any(), any(), any(), any()))
            .thenReturn("payment-proofs/1/order-10.png")
        whenever(mockOrderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

        // -- execute --
        val result = service.uploadPaymentProof(10L, userId, file)

        // -- verify --
        assertThat(result.paymentProofUrl).isEqualTo("payment-proofs/1/order-10.png")
        val fileName = argumentCaptor<String>()
        verify(mockSupabaseService).uploadFile(eq(BUCKET_PAYMENT_PROOFS), fileName.capture(), eq(file), eq(false))
        assertThat(fileName.firstValue).startsWith("1/order-10-").endsWith(".png")
    }

    @Test
    fun `upload payment proof after payment was confirmed`() = runTest {
        val order = OrderTestFixtures.order(status = OrderStatus.PAYMENT_CONFIRMED)
        val file = MockMultipartFile("file", "receipt.png", "image/png", ByteArray(16))
        whenever(mockOrderRepository.findByIdAndProfileId(10L, userId)).thenReturn(order)

        // -- execute --
        assertThrows<IllegalArgumentException> { service.uploadPaymentProof(10L, userId, file) }

        // -- verify nothing was uploaded --
        verify(mockSupabaseService, never()).uploadFile(any(), any(), any(), any())
    }

    @Test
    fun `upload payment proof with an unsupported file type`() = runTest {
        val file = MockMultipartFile("file", "script.sh", "text/x-sh", ByteArray(16))

        // -- execute --
        assertThrows<IllegalArgumentException> { service.uploadPaymentProof(10L, userId, file) }

        // -- verify --
        verify(mockOrderRepository, never()).findByIdAndProfileId(any(), any())
        verify(mockSupabaseService, never()).uploadFile(any(), any(), any(), any())
    }
    // -- end of region payment proof --

    /**
     * a private function to mock what [OrderService.create] reads and writes.
     */
    private fun mockCreateDependencies(user: com.mondi.machine.auths.users.User, products: List<Product>) {
        whenever(mockProfileService.get(userId)).thenReturn(OrderTestFixtures.profile(user))
        whenever(mockAddressService.getById(5L, userId)).thenReturn(OrderTestFixtures.address(user))
        whenever(mockProductRepository.findAllByIdInForUpdate(any())).thenReturn(products)
        whenever(mockOrderRepository.save(any<Order>())).thenAnswer { invocation ->
            (invocation.arguments[0] as Order).apply {
                this.id = 10L
                this.items.forEachIndexed { index, item -> item.id = 100L + index }
            }
        }
    }

    /**
     * a private function to build a one-line [CreateOrderRequest].
     */
    private fun createRequest(productId: Long = 1L, quantity: Int = 1): CreateOrderRequest {
        return CreateOrderRequest(
            addressId = 5L,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            voucherCode = null,
            notes = null,
            items = listOf(CreateOrderItemRequest(productId = productId, quantity = quantity))
        )
    }
}
