package com.mondi.machine.backoffices.products

import com.mondi.machine.backoffices.toNotNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for backoffice related to product.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
@RestController
@RequestMapping("/v1/backoffice/products")
class BackofficeProductController(private val service: BackofficeProductService) : BackofficeProductSwaggerController {

    /**
     * a POST request to handle create new product data.
     *
     * @param request the [BackofficeProductNullableRequest].
     * @return the [BackofficeProductResponse].
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    override suspend fun create(@ModelAttribute request: BackofficeProductNullableRequest): BackofficeProductResponse {
        // -- create the data --
        return service.create(request.toNotNull())
    }

    /**
     * a PUT request to handle update the existing product data.
     *
     * @param productId the product unique identifier.
     * @param request the [BackofficeProductNullableRequest] instance.
     * @return the [BackofficeProductResponse] instance.
     */
    @PutMapping(value = ["/{productId}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override suspend fun put(
        @PathVariable productId: Long,
        @ModelAttribute request: BackofficeProductNullableRequest
    ): BackofficeProductResponse {
        // -- update the data --
        return service.update(productId, request.toNotNull())
    }

    /**
     * a DELETE request to handle delete the existing product data.
     *
     * @param productId the product unique identifier.
     */
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun delete(@PathVariable productId: Long) {
        // -- delete the data --
        service.delete(productId)
    }
}
