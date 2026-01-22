package com.mondi.machine.products

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
    private val mediaRepository: ProductMediaRepository
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
     * @param search the parameter to filter data by name or description.
     * @param category the parameter to filter data by category.
     * @param minPrice the minimum price to filter data.
     * @param maxPrice the maximum price to filter data.
     * @param pageable the [Pageable].
     * @return the [Page] of [ProductResponse].
     */
    fun findAll(
        search: String?,
        category: ProductCategory?,
        minPrice: BigDecimal,
        maxPrice: BigDecimal,
        pageable: Pageable
    ): Page<ProductResponse> {
        // -- find the data --
        return repository.findAllCustom(search, category, minPrice, maxPrice, pageable)
            .map { it.toResponse() }
    }

    /**
     * a function to handle create new [Product].
     *
     * @param name the name of product.
     * @param description the description of product.
     * @param price the price of product.
     * @param currency the currency of product.
     * @param specificationInHtml the specification in HTML format.
     * @param discountPercentage the discount percentage.
     * @param mediaFiles the list of [MultipartFile] for product media.
     * @param category the category of product.
     * @param stock the stock quantity.
     * @return the [Product] instance.
     */
    @Transactional
    suspend fun create(
        name: String,
        description: String?,
        price: BigDecimal,
        currency: String,
        specificationInHtml: String?,
        discountPercentage: BigDecimal,
        mediaFiles: List<MultipartFile>,
        category: ProductCategory,
        stock: Int
    ): Product {
        // -- setup the instance of Product --
        val product = Product(
            name = name,
            description = description,
            price = price,
            currency = currency,
            specificationInHtml = specificationInHtml,
            discountPercentage = discountPercentage,
            category = category,
            stock = stock
        )
        // -- save the instance to database --
        val savedProduct = repository.save(product)
        // -- upload and save media files --
        uploadAndSaveMediaFiles(savedProduct, mediaFiles)
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
        // -- update the instance --
        product.apply {
            this.name = request.name
            this.description = request.description
            this.price = request.price
            this.currency = request.currency
            this.specificationInHtml = request.specificationInHtml
            this.discountPercentage = request.discountPercentage
            this.category = request.category
            this.stock = request.stock
        }
        // -- save the updated instance --
        val updatedProduct = repository.save(product)
        // -- delete old media --
        mediaRepository.deleteByProductId(id)
        // -- upload and save new media files --
        uploadAndSaveMediaFiles(updatedProduct, request.mediaFiles)
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
            // -- create ProductMedia instance --
            val productMedia = ProductMedia(mediaKey, index, product)
            // -- save to database --
            mediaRepository.save(productMedia)
        }
    }

}
