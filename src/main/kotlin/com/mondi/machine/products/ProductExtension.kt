package com.mondi.machine.products

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * an extension function to map the [Product] to [ProductResponse].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
fun Product.toResponse(): ProductResponse {
    val id = this.id
    // -- validate field id --
    requireNotNull(id) {
        "field id is null"
    }
    // -- use stored discount price (no calculation needed, preserves exact value) --
    // -- return the mapped value --
    return ProductResponse(
        id,
        this.name,
        this.description,
        this.price,
        this.discountPrice,
        this.currency,
        this.specificationInHtml,
        this.discountPercentage,
        this.media.map { it.mediaUrl },
        this.category,
        this.stock,
        this.sku,
        this.status,
        this.createdAt
    )
}

/**
 * a function to sanitize file name by removing special characters.
 *
 * @param fileName the original file name.
 * @return the sanitized file name.
 */
fun String.sanitizeFileName(): String {
    return this.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}

/**
 * Calculates the discount percentage.
 * 'this' represents the discounted price.
 */
fun BigDecimal.calculateDiscountPercentage(
    originalPrice: BigDecimal,
    scale: Int = 2
): BigDecimal {
    if (originalPrice <= BigDecimal.ZERO) {
        return BigDecimal.ZERO.setScale(scale)
    }

    val discount = originalPrice - this

    // Percentage = (Discount / Original) * 100
    return (discount * BigDecimal(100))
        .divide(originalPrice, scale, RoundingMode.HALF_UP)
}

/**
 * Calculates the final discount percentage based on the provided inputs.
 *
 * @param originalPrice the original price.
 * @param discountPrice the discounted price.
 * @param discountPercentage the discount percentage.
 * @return the final discount percentage.
 */
fun getFinalDiscountPercentage(
    originalPrice: BigDecimal,
    discountPrice: BigDecimal,
    discountPercentage: BigDecimal
): BigDecimal {
    return when {
        // -- If discountPrice is present (> 0), calculate percentage from it --
        discountPrice.signum() > 0 && originalPrice.signum() > 0 -> {
            discountPrice.calculateDiscountPercentage(originalPrice)
        }
        // -- If discountPrice is missing/zero, use the provided percentage --
        discountPercentage.signum() > 0 -> {
            discountPercentage
        }
        // -- Fallback: No discount info provided --
        else -> BigDecimal.ZERO
    }
}

/**
 * a function to calculate the discount price.
 *
 * @param originalPrice the original price.
 * @param discountPercentage the discount percentage.
 * @return the discount price.
 */
fun getDiscountPrice(originalPrice: BigDecimal, discountPercentage: BigDecimal): BigDecimal {
    val discountAmount = (originalPrice * discountPercentage)
        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    return originalPrice - discountAmount
}
