package com.mondi.machine.reviews

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.accounts.profiles.ProfileService
import com.mondi.machine.exceptions.ReviewAlreadyExistsException
import com.mondi.machine.orders.OrderCustomerResponse
import com.mondi.machine.orders.OrderRepository
import com.mondi.machine.orders.OrderStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The service class for product [Review] operations.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Service
class ReviewService(
    private val repository: ReviewRepository,
    private val orderRepository: OrderRepository,
    private val profileService: ProfileService
) {

    private val log = LoggerFactory.getLogger(ReviewService::class.java)

    /**
     * a function to review a product the user received.
     * The order must belong to the user, be delivered and contain the product.
     * A user can review each product once.
     *
     * @param userId the user unique identifier.
     * @param request the [ReviewRequest] instance.
     * @return the created [ReviewResponse].
     */
    @Transactional
    fun create(userId: Long, request: ReviewRequest): ReviewResponse {
        // -- validate the input --
        require(request.rating in 1..5) {
            "field 'rating' must be between 1 and 5"
        }
        val comment = request.comment?.trim()?.ifEmpty { null }
        require(comment == null || comment.length <= MAX_COMMENT_LENGTH) {
            "field 'comment' cannot be longer than $MAX_COMMENT_LENGTH characters"
        }
        // -- the order must be the user's, delivered, and contain the product --
        val order = orderRepository.findByIdAndProfileId(request.orderId, userId)
            ?: throw NoSuchElementException("no order was found with id '${request.orderId}'")
        require(order.status == OrderStatus.DELIVERED) {
            "you can review a product once the order is delivered"
        }
        val product = order.items.firstOrNull { it.product?.id == request.productId }?.product
            ?: throw IllegalArgumentException("product '${request.productId}' is not part of order '${request.orderId}'")
        // -- one review per product --
        if (repository.existsByProfileIdAndProductId(userId, request.productId)) {
            throw ReviewAlreadyExistsException()
        }
        val review = Review(
            profile = profileService.get(userId),
            product = product,
            order = order,
            rating = request.rating,
            comment = comment
        )
        // -- the unique index also catches two requests at the same time --
        val saved = try {
            repository.saveAndFlush(review)
        } catch (e: DataIntegrityViolationException) {
            throw ReviewAlreadyExistsException()
        }
        log.info("User $userId reviewed product ${request.productId} with ${request.rating} star(s)")
        return saved.toResponse()
    }

    /**
     * a function to get the reviews of a product with the average rating and count.
     *
     * @param productId the product unique identifier.
     * @param pageable the [Pageable].
     * @return the [ProductReviewsResponse].
     */
    @Transactional(readOnly = true)
    fun getByProduct(productId: Long, pageable: Pageable): ProductReviewsResponse {
        val summary = repository.summarizeByProductId(productId)
        return ProductReviewsResponse(
            averageRating = summary.average?.let { Math.round(it * 10) / 10.0 },
            reviewCount = summary.count,
            reviews = repository.findAllByProductId(productId, pageable).map { it.toResponse() }
        )
    }

    /**
     * a function to find all reviews written by the user.
     *
     * @param userId the user unique identifier.
     * @return the list of [ReviewResponse].
     */
    @Transactional(readOnly = true)
    fun findAllByUser(userId: Long): List<ReviewResponse> {
        return repository.findAllByProfileId(userId).map { it.toResponse() }
    }

    /**
     * a function to find all reviews for the backoffice.
     *
     * @param search the search term, or null.
     * @param rating the rating to filter by, or null.
     * @param pageable the [Pageable].
     * @return the [Page] of [BackofficeReviewResponse].
     */
    @Transactional(readOnly = true)
    fun findAll(search: String?, rating: Int?, pageable: Pageable): Page<BackofficeReviewResponse> {
        val term = search?.trim()?.ifEmpty { null }
        return repository.findAllCustom(term, rating, pageable).map { it.toBackofficeResponse() }
    }

    /**
     * a function to delete a review (backoffice moderation).
     *
     * @param reviewId the review unique identifier.
     */
    @Transactional
    fun delete(reviewId: Long) {
        val review = repository.findByIdOrNull(reviewId)
            ?: throw NoSuchElementException("no review was found with id '$reviewId'")
        repository.delete(review)
        log.info("Deleted review $reviewId")
    }

    companion object {
        const val MAX_COMMENT_LENGTH = 2000
    }
}

/**
 * an extension function to map the [Review] to the public [ReviewResponse].
 *
 * @return the [ReviewResponse] instance.
 */
fun Review.toResponse(): ReviewResponse {
    return ReviewResponse(
        id = requireNotNull(this.id) { "field id is null" },
        productId = this.product?.id,
        orderId = this.order?.id,
        rating = this.rating,
        comment = this.comment,
        reviewerName = this.profile.displayName(),
        createdAt = this.createdAt
    )
}

/**
 * an extension function to map the [Review] to [BackofficeReviewResponse].
 *
 * @return the [BackofficeReviewResponse] instance.
 */
fun Review.toBackofficeResponse(): BackofficeReviewResponse {
    return BackofficeReviewResponse(
        id = requireNotNull(this.id) { "field id is null" },
        productId = this.product?.id,
        productName = this.product?.name,
        orderId = this.order?.id,
        rating = this.rating,
        comment = this.comment,
        customer = OrderCustomerResponse(
            id = requireNotNull(this.profile.id) { "field profile id is null" },
            email = this.profile.user.email,
            name = this.profile.name,
            mobile = this.profile.user.mobile
        ),
        createdAt = this.createdAt
    )
}

/**
 * the public name of a reviewer: first name and last initial, e.g. "Jane D.".
 *
 * @return the display name.
 */
fun Profile.displayName(): String {
    val parts = this.name?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    return when {
        parts.isEmpty() -> "Mondi customer"
        parts.size == 1 -> parts[0]
        else -> "${parts.first()} ${parts.last().first().uppercaseChar()}."
    }
}
