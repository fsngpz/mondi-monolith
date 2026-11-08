package com.mondi.machine.reviews

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for product reviews.
 * Reading a product's reviews is public (any GET under /v1/products); writing needs a signed-in customer.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Tag(name = "Review API")
@RestController
class ReviewController(private val service: ReviewService) {

    /**
     * a POST request to review a product from a delivered order.
     */
    @Operation(
        summary = "Review a product",
        description = "Rating 1-5 and an optional comment. The order must be yours, delivered, and contain the product. One review per product (409 otherwise)."
    )
    @PostMapping("/v1/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: ReviewRequest,
        @Parameter(hidden = true) @RequestAttribute("ID") userId: Long
    ): ReviewResponse {
        // -- create the review --
        return service.create(userId, request)
    }

    /**
     * a GET request to list the reviews of a product with the rating summary. Public.
     */
    @Operation(summary = "List product reviews", description = "Public. Newest first, with averageRating and reviewCount.")
    @GetMapping("/v1/products/{productId}/reviews")
    fun getByProduct(
        @PathVariable productId: Long,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ProductReviewsResponse {
        // -- get the reviews --
        return service.getByProduct(productId, pageable)
    }

    /**
     * a GET request to list the reviews written by the signed-in customer.
     */
    @Operation(summary = "List my reviews", description = "Reviews written by the authenticated user")
    @GetMapping("/v1/account/reviews")
    fun findMine(@Parameter(hidden = true) @RequestAttribute("ID") userId: Long): List<ReviewResponse> {
        // -- find my reviews --
        return service.findAllByUser(userId)
    }
}
