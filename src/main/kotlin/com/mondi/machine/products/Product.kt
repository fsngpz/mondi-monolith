package com.mondi.machine.products

import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal

/**
 * The entity class for Product.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "products")
class Product(
    var name: String,
    var description: String?,
    var currency: String,
    var specificationInHtml: String?,

    var price: BigDecimal,
    var discountPercentage: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var category: ProductCategory,

    var stock: Int = 0,

    var sku: String,

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var status: ProductStatus = ProductStatus.ACTIVE
) : AuditableBaseEntity<String>() {

    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
    val media: MutableList<ProductMedia> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Product

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
