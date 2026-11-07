package com.mondi.machine.orders

import com.mondi.machine.TestMondiApplication
import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.accounts.profiles.ProfileRepository
import com.mondi.machine.auths.users.OAuthProvider
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRepository
import com.mondi.machine.configs.AuditorConfig
import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductCategory
import com.mondi.machine.products.ProductRepository
import com.mondi.machine.utils.Currency
import jakarta.persistence.EntityManager
import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.TestPropertySource

/**
 * Runs the Flyway migrations on a real PostgreSQL (Testcontainers) and checks the order
 * entities, enum mappings and repository queries against it.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMondiApplication::class, AuditorConfig::class)
@TestPropertySource(properties = ["spring.flyway.enabled=true"])
internal class OrderPersistenceIntegrationTest(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val productRepository: ProductRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val profileRepository: ProfileRepository,
    @Autowired private val entityManager: EntityManager
) {

    private lateinit var profile: Profile
    private lateinit var product: Product

    @BeforeEach
    fun setUp() {
        val user = userRepository.save(
            User(email = "buyer@example.com", password = "secret", provider = OAuthProvider.LOCAL)
        )
        profile = profileRepository.save(Profile(user).apply { name = "Buyer Name" })
        product = productRepository.save(
            Product(
                name = "Solitaire Ring",
                description = null,
                currency = Currency.IDR,
                specificationInHtml = null,
                price = BigDecimal("1500000"),
                discountPrice = BigDecimal.ZERO,
                category = ProductCategory.RING,
                stock = 4,
                sku = "RING_26_TEST1"
            )
        )
    }

    @Test
    fun `order with items round-trips through the database`() {
        val saved = saveOrder(OrderStatus.PENDING_PAYMENT)
        entityManager.flush()
        entityManager.clear()

        // -- execute --
        val found = orderRepository.findByIdAndProfileId(requireNotNull(saved.id), requireNotNull(profile.id))

        // -- verify --
        assertThat(found).isNotNull
        assertThat(found!!.status).isEqualTo(OrderStatus.PENDING_PAYMENT)
        assertThat(found.paymentMethod).isEqualTo(PaymentMethod.WHATSAPP)
        assertThat(found.items).hasSize(1)
        assertThat(found.items.single().product?.id).isEqualTo(product.id)
        assertThat(found.creatorId).isNotBlank()
        assertThat(found.toResponse().customer?.email).isEqualTo("buyer@example.com")
    }

    @Test
    fun `order of another profile is not found`() {
        val saved = saveOrder(OrderStatus.PENDING_PAYMENT)

        // -- execute --
        val found = orderRepository.findByIdAndProfileId(requireNotNull(saved.id), 999_999L)

        // -- verify --
        assertThat(found).isNull()
    }

    @Test
    fun `find all by profile filters by status`() {
        saveOrder(OrderStatus.PENDING_PAYMENT)
        saveOrder(OrderStatus.DELIVERED)
        val profileId = requireNotNull(profile.id)
        val pageable = PageRequest.of(0, 10)

        // -- execute --
        val all = orderRepository.findAllByProfileId(profileId, null, pageable)
        val delivered = orderRepository.findAllByProfileId(profileId, OrderStatus.DELIVERED, pageable)

        // -- verify --
        assertThat(all.totalElements).isEqualTo(2)
        assertThat(delivered.content.map { it.status }).containsExactly(OrderStatus.DELIVERED)
    }

    @Test
    fun `backoffice search matches id, recipient and customer email`() {
        val saved = saveOrder(OrderStatus.PROCESSING)
        saveOrder(OrderStatus.CANCELLED)
        val pageable = PageRequest.of(0, 10)

        // -- execute & verify --
        assertThat(orderRepository.findAllCustom(null, null, pageable).totalElements).isEqualTo(2)
        assertThat(orderRepository.findAllCustom(null, OrderStatus.PROCESSING, pageable).totalElements).isEqualTo(1)
        assertThat(orderRepository.findAllCustom(saved.id.toString(), null, pageable).content.map { it.id })
            .containsExactly(saved.id)
        assertThat(orderRepository.findAllCustom("recipient", null, pageable).totalElements).isEqualTo(2)
        assertThat(orderRepository.findAllCustom("BUYER@example", null, pageable).totalElements).isEqualTo(2)
        assertThat(orderRepository.findAllCustom("nobody", null, pageable).totalElements).isEqualTo(0)
    }

    @Test
    fun `products can be locked for update`() {
        // -- execute --
        val locked = productRepository.findAllByIdInForUpdate(listOf(requireNotNull(product.id)))

        // -- verify --
        assertThat(locked.map { it.id }).containsExactly(product.id)
    }

    /**
     * a private function to save an order with one item of [product].
     */
    private fun saveOrder(status: OrderStatus): Order {
        val order = Order(
            profile = profile,
            paymentMethod = PaymentMethod.WHATSAPP,
            shippingRecipientName = "Recipient Name",
            // -- no digits, so searching by order id only matches the id --
            shippingPhone = "phone-not-set",
            shippingAddressLine1 = "Jl. Sudirman 1",
            shippingAddressLine2 = null,
            shippingCity = "Jakarta",
            shippingState = "DKI Jakarta",
            shippingPostalCode = "10220",
            shippingCountry = "Indonesia",
            subtotal = BigDecimal("1500000"),
            totalPrice = BigDecimal("1500000")
        ).apply { this.status = status }
        order.items.add(
            OrderItem(
                order = order,
                product = product,
                productName = product.name,
                productSku = product.sku,
                price = product.price,
                quantity = 1
            )
        )
        return orderRepository.save(order)
    }
}
