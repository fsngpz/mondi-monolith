package com.mondi.machine.reviews

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.orders.Order
import com.mondi.machine.products.Product
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The entity class for a product Review.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "reviews")
class Review(
    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    // -- nullable: set to null if the product or order is deleted --
    @ManyToOne
    @JoinColumn(name = "product_id")
    val product: Product?,

    @ManyToOne
    @JoinColumn(name = "order_id")
    val order: Order?,

    var rating: Int,
    var comment: String?
) : AuditableBaseEntity<String>()
