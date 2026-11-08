package com.mondi.machine.orders

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * The interface for repository [Order].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
interface OrderRepository : JpaRepository<Order, Long> {

    /**
     * a function to find an [Order] that belongs to a specific profile.
     *
     * @param id the order unique identifier.
     * @param profileId the profile / user unique identifier.
     * @return the [Order] or null.
     */
    fun findByIdAndProfileId(id: Long, profileId: Long): Order?

    /**
     * a function to check whether a payment proof key belongs to an order of the profile.
     *
     * @param profileId the profile / user unique identifier.
     * @param paymentProofUrl the storage key.
     * @return true if it does.
     */
    fun existsByProfileIdAndPaymentProofUrl(profileId: Long, paymentProofUrl: String): Boolean

    /**
     * a function to find all [Order] of a profile, optionally filtered by status.
     *
     * @param profileId the profile / user unique identifier.
     * @param status the status to filter by, or null for all.
     * @param pageable the [Pageable].
     * @return the [Page] of [Order].
     */
    @Query(
        """
    FROM CustomerOrder o
    WHERE o.profile.id = :profileId
    AND (o.status = COALESCE(:status, o.status))
  """
    )
    fun findAllByProfileId(profileId: Long, status: OrderStatus?, pageable: Pageable): Page<Order>

    /**
     * a function to find all [Order] for the backoffice, filtered by status and a search term
     * matched against the order id, recipient name, phone and the customer email / name.
     *
     * @param search the search term, or null.
     * @param status the status to filter by, or null for all.
     * @param pageable the [Pageable].
     * @return the [Page] of [Order].
     */
    @Query(
        """
    SELECT o FROM CustomerOrder o
    LEFT JOIN o.profile p
    LEFT JOIN p.user u
    WHERE (o.status = COALESCE(:status, o.status))
    AND ((:search IS NULL) OR (
        CAST(o.id AS String) = :search
        OR o.shippingRecipientName ILIKE %:#{#search}%
        OR o.shippingPhone ILIKE %:#{#search}%
        OR u.email ILIKE %:#{#search}%
        OR p.name ILIKE %:#{#search}%))
  """
    )
    fun findAllCustom(search: String?, status: OrderStatus?, pageable: Pageable): Page<Order>
}
