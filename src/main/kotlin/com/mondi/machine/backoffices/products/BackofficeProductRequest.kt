package com.mondi.machine.backoffices.products

import com.mondi.machine.products.ProductCategory
import com.mondi.machine.utils.Currency
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

/**
 * The model class for backoffice product request.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
data class BackofficeProductRequest(
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val currency: Currency,
    val specificationInHtml: String?,
    val discountPercentage: BigDecimal,
    val mediaFiles: List<MultipartFile>,
    val category: ProductCategory,
    val stock: Int
)
