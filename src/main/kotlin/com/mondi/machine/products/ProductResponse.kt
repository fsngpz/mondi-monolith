package com.mondi.machine.products

import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * The model class for response Product.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val discountPrice: BigDecimal,
    val currency: String,
    val specificationInHtml: String?,
    val discountPercentage: BigDecimal,
    val mediaUrls: List<String>,
    val category: ProductCategory,
    val stock: Int,
    val sku: String,
    val status: ProductStatus,
    val createdAt: OffsetDateTime?
)
