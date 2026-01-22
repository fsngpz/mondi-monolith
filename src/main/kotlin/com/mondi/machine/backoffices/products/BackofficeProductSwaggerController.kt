package com.mondi.machine.backoffices.products

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType

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

    @Operation(summary = "Delete product")
    fun delete(@Parameter(description = "Product ID") productId: Long)
}
