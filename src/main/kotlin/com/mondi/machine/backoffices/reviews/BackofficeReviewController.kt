package com.mondi.machine.backoffices.reviews

import com.mondi.machine.reviews.BackofficeReviewResponse
import com.mondi.machine.reviews.ReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for backoffice review moderation.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Tag(name = "Backoffice Review API")
@RestController
@RequestMapping("/v1/backoffice/reviews")
class BackofficeReviewController(private val service: ReviewService) {

    /**
     * a GET request to find all reviews.
     */
    @Operation(
        summary = "List reviews",
        description = "Paginated. `search` matches product name, comment, customer email or name; `rating` filters by stars."
    )
    @GetMapping
    fun findAll(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) rating: Int?,
        @PageableDefault(sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<BackofficeReviewResponse> {
        // -- find all reviews --
        return service.findAll(search, rating, pageable)
    }

    /**
     * a DELETE request to remove a review.
     */
    @Operation(summary = "Delete review")
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable reviewId: Long) {
        // -- delete the review --
        service.delete(reviewId)
    }
}
