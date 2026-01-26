package com.mondi.machine.backoffices.products

import com.mondi.machine.backoffices.toResponse
import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductCategory
import com.mondi.machine.products.ProductService
import com.mondi.machine.products.ProductStatus
import com.mondi.machine.products.calculateDiscountPercentage
import com.mondi.machine.products.getDiscountPrice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * The service class for backoffice related to product.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
@Service
class BackofficeProductService(private val productService: ProductService) {

    /**
     * a function to handle request create new product.
     *
     * @param request the [BackofficeProductRequest] instance.
     * @return the [BackofficeProductResponse] instance.
     */
    suspend fun create(request: BackofficeProductRequest): BackofficeProductResponse {
        // -- create new product --
        return productService.create(request).toResponse()
    }

    /**
     * a function to do an update of [Product] with media management.
     * Keeps existing media by URLs and uploads new media files.
     *
     * @param id the [Product] unique identifier.
     * @param request the [BackofficeProductUpdateRequest] instance.
     * @return the [BackofficeProductResponse] instance.
     */
    suspend fun updateWithMediaManagement(
        id: Long,
        request: BackofficeProductUpdateRequest
    ): BackofficeProductResponse {
        val price = request.price
        val inputDiscountPrice = request.discountPrice ?: BigDecimal.ZERO
        val inputPercent = request.discountPercentage

        // -- calculate both discount price and percentage based on input --
        val (finalDiscountPrice, finalPercentage) = when {
            // Priority 1: Use discount price if provided
            inputDiscountPrice.signum() > 0 -> {
                val percentage = inputDiscountPrice.calculateDiscountPercentage(price)
                Pair(inputDiscountPrice, percentage)
            }
            // Priority 2: Use percentage if provided
            inputPercent.signum() > 0 -> {
                val discountPrice = getDiscountPrice(price, inputPercent)
                Pair(discountPrice, inputPercent)
            }
            // No discount
            else -> Pair(BigDecimal.ZERO, BigDecimal.ZERO)
        }

        // -- make an update to the specified product --
        return productService.updateWithMediaManagement(
            id = id,
            name = request.name,
            description = request.description,
            price = request.price,
            discountPrice = finalDiscountPrice,
            currency = request.currency.name,
            specificationInHtml = request.specificationInHtml,
            discountPercentage = finalPercentage,
            category = request.category,
            stock = request.stock,
            existingMediaUrls = request.existingMediaUrls ?: emptyList(),
            newMediaFiles = request.newMediaFiles ?: emptyList(),
            status = request.status
        ).toResponse()
    }

    /**
     * a function to delete a [Product].
     *
     * @param id the [Product] unique identifier.
     */
    fun delete(id: Long) {
        // -- delete the product --
        productService.delete(id)
    }

    /**
     * a function to find all [Product] with filters.
     *
     * @param search the parameter to filter data by name, description, or specificationInHtml.
     * @param category the parameter to filter data by category.
     * @param minPrice the minimum price to filter data.
     * @param maxPrice the maximum price to filter data.
     * @param status the status to filter data.
     * @param pageable the [Pageable].
     * @return the [Page] of [BackofficeProductResponse].
     */
    fun findAll(
        search: String?,
        category: ProductCategory?,
        minPrice: BigDecimal,
        maxPrice: BigDecimal,
        status: ProductStatus?,
        pageable: Pageable
    ): Page<BackofficeProductResponse> {
        // -- find all products --
        return productService.findAll(search, category, minPrice, maxPrice, status, pageable)
            .map { productResponse ->
                BackofficeProductResponse(
                    id = productResponse.id,
                    name = productResponse.name,
                    description = productResponse.description,
                    price = productResponse.price,
                    discountPrice = productResponse.discountPrice,
                    currency = productResponse.currency,
                    specificationInHtml = productResponse.specificationInHtml,
                    discountPercentage = productResponse.discountPercentage,
                    mediaUrls = productResponse.mediaUrls,
                    category = productResponse.category,
                    stock = productResponse.stock,
                    sku = productResponse.sku,
                    status = productResponse.status,
                    createdAt = productResponse.createdAt
                )
            }
    }
}
