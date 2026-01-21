package com.mondi.machine.backoffices.products

import com.mondi.machine.backoffices.toResponse
import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductService
import org.springframework.stereotype.Service

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
    return productService.create(
      request.name,
      request.description,
      request.price,
      request.currency,
      request.specificationInHtml,
      request.discountPercentage,
      request.mediaFiles,
      request.category,
      request.stock
    ).toResponse()
  }

  /**
   * a function to do an update of [Product].
   *
   * @param id the [Product] unique identifier.
   * @param request the [BackofficeProductRequest] instance.
   * @return the [BackofficeProductResponse] instance.
   */
  suspend fun update(id: Long, request: BackofficeProductRequest): BackofficeProductResponse {
    // -- setup the instance ProductRequest --
    val productRequest = com.mondi.machine.products.ProductRequest(
      name = request.name,
      description = request.description,
      price = request.price,
      currency = request.currency,
      specificationInHtml = request.specificationInHtml,
      discountPercentage = request.discountPercentage,
      mediaFiles = request.mediaFiles,
      category = request.category,
      stock = request.stock
    )
    // -- make an update to the specified product --
    return productService.update(id, productRequest).toResponse()
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
}
