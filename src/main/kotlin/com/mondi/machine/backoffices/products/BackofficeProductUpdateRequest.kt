package com.mondi.machine.backoffices.products

import com.mondi.machine.products.ProductCategory
import com.mondi.machine.products.ProductStatus
import com.mondi.machine.utils.Currency
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

/**
 * The model class for backoffice product update request.
 * Supports keeping existing media by URLs and uploading new media files.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
data class BackofficeProductUpdateRequest(
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val currency: Currency,
    val specificationInHtml: String?,
    val discountPercentage: BigDecimal,
    val discountPrice: BigDecimal?,
    val existingMediaUrls: List<String>?,
    val newMediaFiles: List<MultipartFile>?,
    val category: ProductCategory,
    val stock: Int,
    val status: ProductStatus
)

/**
 * The nullable version for multipart form data binding.
 */
data class BackofficeProductUpdateNullableRequest(
    val name: String?,
    val description: String?,
    val price: BigDecimal?,
    val currency: Currency?,
    val specificationInHtml: String?,
    val discountPercentage: BigDecimal?,
    val discountPrice: BigDecimal?,
    val existingMediaUrls: List<String>?,
    val newMediaFiles: List<MultipartFile>?,
    val category: ProductCategory?,
    val stock: Int?,
    val status: ProductStatus?
)
