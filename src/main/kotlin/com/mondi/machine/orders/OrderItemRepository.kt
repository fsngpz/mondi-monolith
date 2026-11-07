package com.mondi.machine.orders

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * The interface for repository [OrderItem].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
interface OrderItemRepository : JpaRepository<OrderItem, Long> {

    /**
     * a function to find all order items of a profile that have a certificate,
     * newest order first, leaving out orders with [excludedStatus].
     * The status is a parameter because an enum literal in JPQL is cast to a PostgreSQL type
     * named after the Kotlin enum, which does not exist.
     *
     * @param profileId the profile / user unique identifier.
     * @param excludedStatus the order status to leave out, usually [OrderStatus.CANCELLED].
     * @return the list of [OrderItem].
     */
    @Query(
        """
    SELECT i FROM OrderItem i
    JOIN i.order o
    WHERE o.profile.id = :profileId
    AND i.certificateUrl IS NOT NULL
    AND o.status <> :excludedStatus
    ORDER BY o.createdAt DESC, i.id ASC
  """
    )
    fun findAllWithCertificateByProfileId(profileId: Long, excludedStatus: OrderStatus): List<OrderItem>

    /**
     * a function to find all order items that have a certificate, for the backoffice.
     * The search term is matched against the product name, SKU, order id and customer email.
     *
     * @param search the search term, or null.
     * @param pageable the [Pageable].
     * @return the [Page] of [OrderItem].
     */
    @Query(
        """
    SELECT i FROM OrderItem i
    JOIN i.order o
    LEFT JOIN o.profile p
    LEFT JOIN p.user u
    WHERE i.certificateUrl IS NOT NULL
    AND ((:search IS NULL) OR (
        CAST(o.id AS String) = :search
        OR i.productName ILIKE %:#{#search}%
        OR i.productSku ILIKE %:#{#search}%
        OR u.email ILIKE %:#{#search}%
        OR p.name ILIKE %:#{#search}%))
  """
    )
    fun findAllWithCertificate(search: String?, pageable: Pageable): Page<OrderItem>
}
