package com.mondi.machine.backoffices.products

import com.mondi.machine.products.ProductCategory
import com.mondi.machine.products.ProductStatus
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * The model class for backoffice product response.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
data class BackofficeProductResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
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
