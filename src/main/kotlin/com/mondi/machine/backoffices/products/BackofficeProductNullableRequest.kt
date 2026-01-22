package com.mondi.machine.backoffices.products

import com.mondi.machine.products.ProductCategory
import com.mondi.machine.utils.Currency
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

/**
 * The model class for backoffice product nullable request.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
data class BackofficeProductNullableRequest(
    @Schema(description = "Product name", example = "Diamond Ring")
    val name: String?,
    @Schema(description = "Product description", example = "Beautiful diamond ring")
    val description: String?,
    @Schema(description = "Product price", example = "1500.00")
    val price: BigDecimal?,
    @Schema(description = "Product currency", example = "USD")
    val currency: Currency?,
    @Schema(description = "Product specification in HTML", example = "<p>14k gold</p>")
    val specificationInHtml: String?,
    @Schema(description = "Product discount percentage", example = "10.00")
    val discountPercentage: BigDecimal?,

    val mediaFiles: List<MultipartFile>?,

    @Schema(description = "Product category", example = "RING")
    val category: ProductCategory?,
    @Schema(description = "Product stock", example = "50")
    val stock: Int?
)
