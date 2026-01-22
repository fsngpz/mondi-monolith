package com.mondi.machine.products

/**
 * an extension function to map the [Product] to [ProductResponse].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
fun Product.toResponse(): ProductResponse {
    // -- validate field id --
    requireNotNull(this.id) {
        "field id is null"
    }
    // -- return the mapped value --
    return ProductResponse(
        this.id!!,
        this.name,
        this.description,
        this.price,
        this.currency,
        this.specificationInHtml,
        this.discountPercentage,
        this.media.map { it.mediaUrl },
        this.category,
        this.stock
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
