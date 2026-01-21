package com.mondi.machine.products

import java.math.BigDecimal
import org.springframework.web.multipart.MultipartFile

/**
 * The model class for product request.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
data class ProductRequest(
  val name: String,
  val description: String?,
  val price: BigDecimal,
  val currency: String,
  val specificationInHtml: String?,
  val discountPercentage: BigDecimal,
  val mediaFiles: List<MultipartFile>,
  val category: ProductCategory,
  val stock: Int
)
