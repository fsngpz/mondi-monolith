package com.mondi.machine.backoffices.products

import com.mondi.machine.products.ProductCategory
import java.math.BigDecimal

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
  val stock: Int
)
