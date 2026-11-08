package com.mondi.machine.reviews

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * The interface for repository [Review].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
interface ReviewRepository : JpaRepository<Review, Long> {

    /**
     * a function to check whether the profile already reviewed the product.
     */
    fun existsByProfileIdAndProductId(profileId: Long, productId: Long): Boolean

    /**
     * a function to find the reviews of a product.
     */
    fun findAllByProductId(productId: Long, pageable: Pageable): Page<Review>

    /**
     * a function to find all reviews written by a profile.
     */
    fun findAllByProfileId(profileId: Long): List<Review>

    /**
     * a function to get the average rating and number of reviews of a product.
     *
     * @param productId the product unique identifier.
     * @return the [RatingSummary]; the average is null without reviews.
     */
    @Query("SELECT AVG(r.rating) AS average, COUNT(r) AS count FROM Review r WHERE r.product.id = :productId")
    fun summarizeByProductId(productId: Long): RatingSummary

    /**
     * a function to find all reviews for the backoffice, filtered by rating and a search term
     * matched against the product name, comment and customer email / name.
     *
     * @param search the search term, or null.
     * @param rating the rating to filter by, or null.
     * @param pageable the [Pageable].
     * @return the [Page] of [Review].
     */
    @Query(
        """
    SELECT r FROM Review r
    LEFT JOIN r.product pr
    JOIN r.profile p
    JOIN p.user u
    WHERE (:rating IS NULL OR r.rating = :rating)
    AND ((:search IS NULL) OR (
        pr.name ILIKE %:#{#search}%
        OR r.comment ILIKE %:#{#search}%
        OR u.email ILIKE %:#{#search}%
        OR p.name ILIKE %:#{#search}%))
  """
    )
    fun findAllCustom(search: String?, rating: Int?, pageable: Pageable): Page<Review>
}

/**
 * The projection of [ReviewRepository.summarizeByProductId].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
interface RatingSummary {
    val average: Double?
    val count: Long
}
