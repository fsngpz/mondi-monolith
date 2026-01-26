package com.mondi.machine.products

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/**
 * The REST controller for product.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
@RestController
@RequestMapping("/v1/products")
class ProductController(private val service: ProductService) : ProductSwaggerController {

    /**
     * a GET request to get the product detail.
     *
     * @param productId the product unique identifier.
     * @return the [ProductResponse] instance.
     */
    @GetMapping("/{productId}")
    override fun get(@PathVariable productId: Long): ProductResponse {
        // -- get the product --
        return service.get(productId).toResponse()
    }

    /**
     * a GET request to find all products with filters.
     *
     * @param search the parameter to filter data by name, description, or specificationInHtml.
     * @param category the parameter to filter data by category.
     * @param minPrice the minimum price to filter data (default: 0).
     * @param maxPrice the maximum price to filter data (default: 999999999).
     * @param status the status to filter data.
     * @param pageable the [Pageable].
     * @return the [Page] of [ProductResponse].
     */
    @GetMapping
    override fun findAll(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) category: ProductCategory?,
        @RequestParam(required = false, defaultValue = "0") minPrice: BigDecimal,
        @RequestParam(required = false, defaultValue = "999999999") maxPrice: BigDecimal,
        @RequestParam(required = false) status: ProductStatus?,
        pageable: Pageable
    ): Page<ProductResponse> {
        // -- find all products --
        return service.findAll(search, category, minPrice, maxPrice, status, pageable)
    }
}
