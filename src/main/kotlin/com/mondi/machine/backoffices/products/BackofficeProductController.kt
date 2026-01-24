package com.mondi.machine.backoffices.products

import com.mondi.machine.backoffices.toNotNull
import com.mondi.machine.products.ProductCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

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
     * a PATCH request to handle update the existing product data with media management.
     * Allows keeping existing media by URLs and uploading new media files.
     *
     * @param productId the product unique identifier.
     * @param request the [BackofficeProductUpdateNullableRequest] instance.
     * @return the [BackofficeProductResponse] instance.
     */
    @PatchMapping(value = ["/{productId}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override suspend fun patchWithMediaManagement(
        @PathVariable productId: Long,
        @ModelAttribute request: BackofficeProductUpdateNullableRequest
    ): BackofficeProductResponse {
        // -- update the data with media management --
        return service.updateWithMediaManagement(productId, request.toNotNull())
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

    /**
     * a GET request to find all products with filters.
     *
     * @param search the parameter to filter data by name, description, or specificationInHtml.
     * @param category the parameter to filter data by category.
     * @param minPrice the minimum price to filter data (default: 0).
     * @param maxPrice the maximum price to filter data (default: 999999999).
     * @param pageable the [Pageable].
     * @return the [Page] of [BackofficeProductResponse].
     */
    @GetMapping
    override fun findAll(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) category: ProductCategory?,
        @RequestParam(required = false, defaultValue = "0") minPrice: BigDecimal,
        @RequestParam(required = false, defaultValue = "999999999") maxPrice: BigDecimal,
        pageable: Pageable
    ): Page<BackofficeProductResponse> {
        // -- find all products --
        return service.findAll(search, category, minPrice, maxPrice, pageable)
    }
}
