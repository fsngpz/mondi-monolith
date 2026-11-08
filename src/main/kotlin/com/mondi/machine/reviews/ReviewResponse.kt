package com.mondi.machine.reviews

import com.mondi.machine.orders.OrderCustomerResponse
import java.time.OffsetDateTime
import org.springframework.data.domain.Page

/**
 * The public response model class for a [Review]. The reviewer is shown as first name and last initial.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class ReviewResponse(
    val id: Long,
    val productId: Long?,
    val orderId: Long?,
    val rating: Int,
    val comment: String?,
    val reviewerName: String,
    val createdAt: OffsetDateTime?
)

/**
 * The response model class for the reviews of a product, with the rating summary.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class ProductReviewsResponse(
    // -- null when the product has no reviews --
    val averageRating: Double?,
    val reviewCount: Long,
    val reviews: Page<ReviewResponse>
)

/**
 * The backoffice response model class for a [Review], with the product and customer.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class BackofficeReviewResponse(
    val id: Long,
    val productId: Long?,
    val productName: String?,
    val orderId: Long?,
    val rating: Int,
    val comment: String?,
    val customer: OrderCustomerResponse,
    val createdAt: OffsetDateTime?
)
