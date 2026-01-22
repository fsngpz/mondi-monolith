package com.mondi.machine.products

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

/**
 * The interface for Product Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-21
 */
@Tag(name = "Product API")
interface ProductSwaggerController {

    @Operation(summary = "Get detail product")
    fun get(productId: Long): ProductResponse

    @Operation(summary = "Get all products with filter")
    fun findAll(
        search: String?,
        category: ProductCategory?,
        minPrice: BigDecimal,
        maxPrice: BigDecimal,
        @ParameterObject pageable: Pageable
    ): Page<ProductResponse>
}
