package com.mondi.machine.products

import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The entity class for Product Media.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "product_media")
class ProductMedia(
  var mediaUrl: String,
  var displayOrder: Int = 0,

  // -- Many to One --
  @ManyToOne
  @JoinColumn(name = "product_id")
  val product: Product
) : AuditableBaseEntity<String>() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ProductMedia

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }
}
