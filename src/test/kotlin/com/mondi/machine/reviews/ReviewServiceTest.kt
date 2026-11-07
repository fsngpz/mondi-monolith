package com.mondi.machine.reviews

import com.mondi.machine.accounts.profiles.ProfileService
import com.mondi.machine.exceptions.ReviewAlreadyExistsException
import com.mondi.machine.orders.OrderRepository
import com.mondi.machine.orders.OrderStatus
import com.mondi.machine.orders.OrderTestFixtures
import com.mondi.machine.orders.OrderTestFixtures.withItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * The test class for [ReviewService].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@SpringBootTest(classes = [ReviewService::class])
internal class ReviewServiceTest(@Autowired private val service: ReviewService) {

    @MockitoBean
    lateinit var mockReviewRepository: ReviewRepository

    @MockitoBean
    lateinit var mockOrderRepository: OrderRepository

    @MockitoBean
    lateinit var mockProfileService: ProfileService

    private val userId = 1L
    private val profile = OrderTestFixtures.profile()
    private val product = OrderTestFixtures.product(id = 3L)

    private fun request(rating: Int = 5, comment: String? = "Beautiful ring") =
        ReviewRequest(productId = 3L, orderId = 10L, rating = rating, comment = comment)

    private fun mockDeliveredOrder(status: OrderStatus = OrderStatus.DELIVERED) {
        val order = OrderTestFixtures.order(profile, status).withItem(product)
        whenever(mockOrderRepository.findByIdAndProfileId(10L, userId)).thenReturn(order)
        whenever(mockProfileService.get(userId)).thenReturn(profile)
        whenever(mockReviewRepository.saveAndFlush(any<Review>())).thenAnswer {
            (it.arguments[0] as Review).apply { id = 50L }
        }
    }

    @Test
    fun `review a delivered product`() {
        mockDeliveredOrder()

        // -- execute --
        val result = service.create(userId, request(comment = "  Beautiful ring  "))

        // -- verify --
        assertThat(result.id).isEqualTo(50L)
        assertThat(result.rating).isEqualTo(5)
        assertThat(result.comment).isEqualTo("Beautiful ring")
        assertThat(result.reviewerName).isEqualTo("Jane D.")
        val saved = argumentCaptor<Review>()
        verify(mockReviewRepository).saveAndFlush(saved.capture())
        assertThat(saved.firstValue.product?.id).isEqualTo(3L)
        assertThat(saved.firstValue.order?.id).isEqualTo(10L)
    }

    @Test
    fun `review a product twice is rejected`() {
        mockDeliveredOrder()
        whenever(mockReviewRepository.existsByProfileIdAndProductId(userId, 3L)).thenReturn(true)

        // -- execute & verify --
        assertThrows<ReviewAlreadyExistsException> { service.create(userId, request()) }
        verify(mockReviewRepository, never()).saveAndFlush(any<Review>())
    }

    @Test
    fun `two reviews at the same time hit the unique index and are rejected`() {
        mockDeliveredOrder()
        whenever(mockReviewRepository.saveAndFlush(any<Review>())).thenThrow(DataIntegrityViolationException("dup"))

        // -- execute & verify --
        assertThrows<ReviewAlreadyExistsException> { service.create(userId, request()) }
    }

    @ParameterizedTest
    @CsvSource("PENDING_PAYMENT", "PAYMENT_CONFIRMED", "PROCESSING", "SHIPPED", "CANCELLED")
    fun `review before delivery is rejected`(status: OrderStatus) {
        mockDeliveredOrder(status)

        // -- execute & verify --
        val exception = assertThrows<IllegalArgumentException> { service.create(userId, request()) }
        assertThat(exception.message).contains("delivered")
        verify(mockReviewRepository, never()).saveAndFlush(any<Review>())
    }

    @Test
    fun `review a product that is not in the order is rejected`() {
        mockDeliveredOrder()

        // -- execute & verify --
        assertThrows<IllegalArgumentException> { service.create(userId, request().copy(productId = 99L)) }
    }

    @Test
    fun `review with an order of another customer is not found`() {
        whenever(mockOrderRepository.findByIdAndProfileId(10L, 2L)).thenReturn(null)

        // -- execute & verify --
        assertThrows<NoSuchElementException> { service.create(2L, request()) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 6, -1])
    fun `rating outside 1 to 5 is rejected`(rating: Int) {
        assertThrows<IllegalArgumentException> { service.create(userId, request(rating = rating)) }
        verify(mockOrderRepository, never()).findByIdAndProfileId(any(), any())
    }

    @Test
    fun `comment longer than the limit is rejected`() {
        assertThrows<IllegalArgumentException> {
            service.create(userId, request(comment = "a".repeat(ReviewService.MAX_COMMENT_LENGTH + 1)))
        }
    }

    @Test
    fun `product reviews include the rounded average and count`() {
        val pageable = PageRequest.of(0, 10)
        val review = Review(profile, product, null, 4, null).apply { id = 1L }
        whenever(mockReviewRepository.summarizeByProductId(3L)).thenReturn(object : RatingSummary {
            override val average: Double = 4.666666
            override val count: Long = 3
        })
        whenever(mockReviewRepository.findAllByProductId(3L, pageable)).thenReturn(PageImpl(listOf(review), pageable, 3))

        // -- execute --
        val result = service.getByProduct(3L, pageable)

        // -- verify --
        assertThat(result.averageRating).isEqualTo(4.7)
        assertThat(result.reviewCount).isEqualTo(3)
        assertThat(result.reviews.content.single().rating).isEqualTo(4)
    }

    @Test
    fun `product without reviews has no average`() {
        val pageable = PageRequest.of(0, 10)
        whenever(mockReviewRepository.summarizeByProductId(3L)).thenReturn(object : RatingSummary {
            override val average: Double? = null
            override val count: Long = 0
        })
        whenever(mockReviewRepository.findAllByProductId(3L, pageable)).thenReturn(PageImpl(emptyList(), pageable, 0))

        // -- execute --
        val result = service.getByProduct(3L, pageable)

        // -- verify --
        assertThat(result.averageRating).isNull()
        assertThat(result.reviewCount).isZero()
    }

    @Test
    fun `reviewer names show the first name and last initial`() {
        fun nameOf(name: String?) = OrderTestFixtures.profile().apply { this.name = name }.displayName()

        assertThat(nameOf("Jane Doe")).isEqualTo("Jane D.")
        assertThat(nameOf("  maria   clara  santoso ")).isEqualTo("maria S.")
        assertThat(nameOf("Cher")).isEqualTo("Cher")
        assertThat(nameOf(null)).isEqualTo("Mondi customer")
        assertThat(nameOf("   ")).isEqualTo("Mondi customer")
    }
}
