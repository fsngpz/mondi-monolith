package com.mondi.machine.products

import org.springframework.data.jpa.repository.JpaRepository

/**
 * The interface for repository [ProductMedia].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
interface ProductMediaRepository : JpaRepository<ProductMedia, Long> {

  /**
   * a function to find all product media by product id.
   *
   * @param productId the product unique identifier.
   * @return the list of [ProductMedia].
   */
  fun findByProductIdOrderByDisplayOrder(productId: Long): List<ProductMedia>

  /**
   * a function to delete all product media by product id.
   *
   * @param productId the product unique identifier.
   */
  fun deleteByProductId(productId: Long)
}
