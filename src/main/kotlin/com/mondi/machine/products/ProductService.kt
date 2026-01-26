package com.mondi.machine.products

import com.mondi.machine.backoffices.products.BackofficeProductRequest
import com.mondi.machine.storage.supabase.SupabaseService
import com.mondi.machine.storage.supabase.SupabaseService.Companion.BUCKET_PRODUCTS
import jakarta.transaction.Transactional
import org.apache.commons.io.FilenameUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * The service class for [Product].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-21
 */
@Service
class ProductService(
    private val supabaseService: SupabaseService,
    private val repository: ProductRepository,
    private val mediaRepository: ProductMediaRepository,
    private val skuGenerationService: SkuGenerationService,
    private val htmlSanitizer: com.mondi.machine.utils.HtmlSanitizer
) {

    /**
     * a function to get the [Product] instance.
     *
     * @param id the unique identifier of [Product].
     * @return the [Product] instance.
     */
    fun get(id: Long): Product {
        return repository.findByIdOrNull(id)
            ?: throw NoSuchElementException("no product data was found with id '$id'")
    }

    /**
     * a function to find all [Product].
     *
     * @param search the parameter to filter data by name, description, or specificationInHtml.
     * @param category the parameter to filter data by category.
     * @param minPrice the minimum price to filter data.
     * @param maxPrice the maximum price to filter data.
     * @param status the status to filter data.
     * @param pageable the [Pageable].
     * @return the [Page] of [ProductResponse].
     */
    fun findAll(
        search: String?,
        category: ProductCategory?,
        minPrice: BigDecimal,
        maxPrice: BigDecimal,
        status: ProductStatus?,
        pageable: Pageable
    ): Page<ProductResponse> {
        // -- find the data --
        return repository.findAllCustom(search, category, minPrice, maxPrice, status, pageable)
            .map { it.toResponse() }
    }

    /**
     * a function to handle create new [Product].
     *
     * @param request the [BackofficeProductRequest] instance.
     * @return the [Product] instance.
     */
    @Transactional
    suspend fun create(request: BackofficeProductRequest): Product {
        val price = request.price
        val inputPrice = request.discountPrice ?: BigDecimal.ZERO
        val inputPercent = request.discountPercentage
        val discountPercentage = getFinalDiscountPercentage(price, inputPrice, inputPercent)

        // -- generate SKU --
        val sku = skuGenerationService.generateSku(request.category)
        // -- sanitize HTML to prevent XSS --
        val sanitizedSpecification = htmlSanitizer.sanitize(request.specificationInHtml)
        // -- setup the instance of Product --
        val product = Product(
            name = request.name,
            description = request.description,
            price = request.price,
            currency = request.currency.name,
            specificationInHtml = sanitizedSpecification,
            discountPercentage = discountPercentage,
            category = request.category,
            stock = request.stock,
            sku = sku,
            status = ProductStatus.ACTIVE
        )
        // -- save the instance to database --
        val savedProduct = repository.save(product)
        // -- upload and save media files --
        if (request.mediaFiles != null) {
            uploadAndSaveMediaFiles(savedProduct, request.mediaFiles)
        }
        // -- return the saved product --
        return savedProduct
    }

    /**
     * a function to update the instance of [Product].
     *
     * @param id the [Product] unique identifier.
     * @param request the [ProductRequest] instance.
     * @return the [Product] instance.
     */
    @Transactional
    suspend fun update(id: Long, request: ProductRequest): Product {
        // -- get the product instance --
        val product = get(id)
        // -- sanitize HTML to prevent XSS --
        val sanitizedSpecification = htmlSanitizer.sanitize(request.specificationInHtml)
        // -- update the instance --
        product.apply {
            this.name = request.name
            this.description = request.description
            this.price = request.price
            this.currency = request.currency
            this.specificationInHtml = sanitizedSpecification
            this.discountPercentage = request.discountPercentage
            this.category = request.category
            this.stock = request.stock
        }
        // -- save the updated instance --
        val updatedProduct = repository.save(product)
        // -- clear old media from collection --
        updatedProduct.media.clear()
        // -- delete old media from database --
        mediaRepository.deleteByProductId(id)
        // -- upload and save new media files --
        uploadAndSaveMediaFiles(updatedProduct, request.mediaFiles)
        // -- return the updated product --
        return updatedProduct
    }

    /**
     * a function to update the instance of [Product] with media management.
     * Keeps existing media by URLs and uploads new media files.
     *
     * @param id the [Product] unique identifier.
     * @param request the product update data.
     * @param existingMediaUrls the list of existing media URLs to keep.
     * @param newMediaFiles the list of new media files to upload.
     * @return the [Product] instance.
     */
    @Transactional
    suspend fun updateWithMediaManagement(
        id: Long,
        name: String,
        description: String?,
        price: BigDecimal,
        currency: String,
        specificationInHtml: String?,
        discountPercentage: BigDecimal,
        category: ProductCategory,
        stock: Int,
        existingMediaUrls: List<String>,
        newMediaFiles: List<MultipartFile>,
        status: ProductStatus
    ): Product {
        // -- get the product instance --
        val product = get(id)

        // -- sanitize HTML to prevent XSS --
        val sanitizedSpecification = htmlSanitizer.sanitize(specificationInHtml)

        // -- update basic fields --
        product.apply {
            this.name = name
            this.description = description
            this.price = price
            this.currency = currency
            this.specificationInHtml = sanitizedSpecification
            this.discountPercentage = discountPercentage
            this.category = category
            this.stock = stock
            this.status = status
        }

        // -- save the updated instance --
        val updatedProduct = repository.save(product)

        // -- handle media updates --
        val currentMedia = updatedProduct.media.toList()

        // -- find media to delete (not in existingMediaUrls) --
        val mediaToDelete = currentMedia.filter { it.mediaUrl !in existingMediaUrls }

        // -- delete media not in the keep list --
        mediaToDelete.forEach { media ->
            mediaRepository.delete(media)
            updatedProduct.media.remove(media)
        }

        // -- find media to keep and update their order --
        val mediaToKeep = currentMedia.filter { it.mediaUrl in existingMediaUrls }
        mediaToKeep.forEachIndexed { index, media ->
            media.displayOrder = index
            mediaRepository.save(media)
        }

        // -- upload and save new media files --
        val startOrder = mediaToKeep.size
        newMediaFiles.forEachIndexed { index, file ->
            val sanitizedFileName = updatedProduct.name.sanitizeFileName()
            val extension = FilenameUtils.getExtension(file.originalFilename)
            val fileName = "${OffsetDateTime.now().toEpochSecond()}-${sanitizedFileName}.${extension}"
            val mediaKey = supabaseService.uploadFile(BUCKET_PRODUCTS, fileName, file)
                ?: throw IllegalArgumentException("the file path on upload product media is null")
            val mediaUrl = supabaseService.getPublicUrl(mediaKey)

            // -- create ProductMedia instance --
            val productMedia = ProductMedia(mediaUrl, startOrder + index, updatedProduct)

            // -- save to database --
            val savedMedia = mediaRepository.save(productMedia)

            // -- add to product's media collection --
            updatedProduct.media.add(savedMedia)
        }

        // -- return the updated product --
        return updatedProduct
    }

    /**
     * a function to delete the [Product].
     *
     * @param id the [Product] unique identifier.
     */
    fun delete(id: Long) {
        // -- get the product instance --
        val product = get(id)
        // -- delete the instance --
        repository.delete(product)
    }

    /**
     * a private function to upload media files and save to database.
     *
     * @param product the [Product] instance.
     * @param mediaFiles the list of [MultipartFile].
     */
    private suspend fun uploadAndSaveMediaFiles(product: Product, mediaFiles: List<MultipartFile>) {
        mediaFiles.forEachIndexed { index, file ->
            val sanitizedFileName = product.name.sanitizeFileName()
            val extension = FilenameUtils.getExtension(file.originalFilename)
            val fileName = "${OffsetDateTime.now().toEpochSecond()}-${sanitizedFileName}.${extension}"
            val mediaKey = supabaseService.uploadFile(BUCKET_PRODUCTS, fileName, file)
                ?: throw IllegalArgumentException("the file path on upload product media is null")
            val mediaUrl = supabaseService.getPublicUrl(mediaKey)
            // -- create ProductMedia instance --
            val productMedia = ProductMedia(mediaUrl, index, product)
            // -- save to database --
            val savedMedia = mediaRepository.save(productMedia)
            // -- add to product's media collection --
            product.media.add(savedMedia)
        }
    }

}
