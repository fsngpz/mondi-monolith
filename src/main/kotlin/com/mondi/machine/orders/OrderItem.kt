package com.mondi.machine.orders

import com.mondi.machine.products.Product
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The entity class for one line of an [Order].
 *
 * Name, SKU and price are copied from the [Product] when the order is placed.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "order_items")
class OrderItem(
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    // -- nullable: the column is set to null if the product is deleted --
    @ManyToOne
    @JoinColumn(name = "product_id")
    val product: Product?,

    // -- snapshots --
    val productName: String,
    val productSku: String,
    val price: BigDecimal,
    val quantity: Int
) : AuditableBaseEntity<String>() {
    // -- the storage key of the certificate uploaded by an admin --
    var certificateUrl: String? = null
}
