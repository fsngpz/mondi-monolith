package com.mondi.machine.products

import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

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
    val discountPrice: BigDecimal?,
    val currency: String,
    val specificationInHtml: String?,
    val discountPercentage: BigDecimal,
    val mediaFiles: List<MultipartFile>,
    val category: ProductCategory,
    val stock: Int
)
