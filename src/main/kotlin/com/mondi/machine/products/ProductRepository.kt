package com.mondi.machine.products

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal

/**
 * The interface for repository [Product].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
interface ProductRepository : JpaRepository<Product, Long> {

    /**
     * a function to find all [Product] with custom logic.
     *
     * @param search the parameter to filter data by name, description, or specificationInHtml.
     * @param category the parameter to filter data by category.
     * @param minPrice the minimum price to filter data.
     * @param maxPrice the maximum price to filter data.
     * @param status the parameter to filter data by status.
     * @param pageable the [Pageable].
     * @return the [Page] of [Product].
     */
    @Query(
        """
    FROM Product p
    WHERE ((:search IS NULL ) OR (p.name ILIKE %:#{#search}% OR p.description ILIKE %:#{#search}% OR p.specificationInHtml ILIKE %:#{#search}%))
    AND (p.category = COALESCE(:category, p.category))
    AND (cast(p.price as bigdecimal) >= :minPrice)
    AND (cast(p.price as bigdecimal) <= :maxPrice)
    AND (p.status = COALESCE(:status, p.status))
  """
    )
    fun findAllCustom(
        search: String?,
        category: ProductCategory?,
        minPrice: BigDecimal,
        maxPrice: BigDecimal,
        status: ProductStatus?,
        pageable: Pageable
    ): Page<Product>

    /**
     * a function to count products by SKU prefix.
     *
     * @param skuPrefix the SKU prefix (e.g., "RING_26_")
     * @return the count of products with the given SKU prefix.
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.sku LIKE CONCAT(:skuPrefix, '%')")
    fun countBySkuPrefix(skuPrefix: String): Long
}
