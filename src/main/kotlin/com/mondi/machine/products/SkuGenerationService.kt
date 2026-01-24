package com.mondi.machine.products

import org.springframework.stereotype.Service
import java.time.Year

/**
 * The service class for SKU generation.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-23
 */
@Service
class SkuGenerationService(
    private val productRepository: ProductRepository
) {

    /**
     * a function to generate SKU for a product.
     *
     * Format: Category_YY_IncrementingNumber
     * Example: RING_26_001, EARRING_26_042
     *
     * @param category the [ProductCategory].
     * @return the generated SKU.
     */
    fun generateSku(category: ProductCategory): String {
        // -- get current year (last 2 digits) --
        val year = Year.now().value % 100
        val yearStr = year.toString().padStart(2, '0')

        // -- create SKU prefix --
        val skuPrefix = "${category.name}_${yearStr}_"

        // -- get count of products with this prefix --
        val count = productRepository.countBySkuPrefix(skuPrefix)

        // -- increment and format the number --
        val incrementingNumber = (count + 1).toString().padStart(3, '0')

        // -- return the complete SKU --
        return "$skuPrefix$incrementingNumber"
    }
}
