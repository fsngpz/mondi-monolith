package com.mondi.machine.backoffices.products

import com.mondi.machine.products.ProductCategory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import java.math.BigDecimal

/**
 * The interface for Backoffice Product Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-21
 */
@Tag(name = "Backoffice Product API")
interface BackofficeProductSwaggerController {

    @Operation(
        summary = "Create new product",
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = Schema(implementation = BackofficeProductNullableRequest::class)
            )]
        )
    )
    suspend fun create(request: BackofficeProductNullableRequest): BackofficeProductResponse

    @Operation(
        summary = "Update existing product",
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = Schema(implementation = BackofficeProductNullableRequest::class)
            )]
        )
    )
    suspend fun put(
        @Parameter(description = "Product ID") productId: Long,
        request: BackofficeProductNullableRequest
    ): BackofficeProductResponse

    @Operation(
        summary = "Update existing product with media management (recommended)",
        description = "Update product while keeping existing media by URLs and uploading new media files. " +
                "Frontend should send existingMediaUrls to keep and newMediaFiles to upload.",
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = Schema(implementation = BackofficeProductUpdateNullableRequest::class)
            )]
        )
    )
    suspend fun patchWithMediaManagement(
        @Parameter(description = "Product ID") productId: Long,
        request: BackofficeProductUpdateNullableRequest
    ): BackofficeProductResponse

    @Operation(summary = "Delete product")
    fun delete(@Parameter(description = "Product ID") productId: Long)

    @Operation(summary = "Get all products with filters")
    fun findAll(
        search: String?,
        category: ProductCategory?,
        minPrice: BigDecimal,
        maxPrice: BigDecimal,
        @ParameterObject pageable: Pageable
    ): Page<BackofficeProductResponse>
}
