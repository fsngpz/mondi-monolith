package com.mondi.machine.products

import com.mondi.machine.storage.supabase.SupabaseService
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.util.Optional

/**
 * The test class of [ProductService].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-22
 */
@SpringBootTest(classes = [ProductService::class])
@ActiveProfiles("test")
internal class ProductServiceTest(@Autowired private val productService: ProductService) {
    // -- region of mock --
    @MockitoBean
    lateinit var mockSupabaseService: SupabaseService

    @MockitoBean
    lateinit var mockProductRepository: ProductRepository

    @MockitoBean
    lateinit var mockProductMediaRepository: ProductMediaRepository

    @MockitoBean
    lateinit var mockSkuGenerationService: SkuGenerationService

    @MockitoBean
    lateinit var mockHtmlSanitizer: com.mondi.machine.utils.HtmlSanitizer
    // -- end of region mock --

    // -- region of smoke testing --
    @Test
    fun `dependencies are not null`() {
        assertThat(productService).isNotNull
        assertThat(mockSupabaseService).isNotNull
        assertThat(mockProductRepository).isNotNull
        assertThat(mockProductMediaRepository).isNotNull
        assertThat(mockSkuGenerationService).isNotNull
        assertThat(mockHtmlSanitizer).isNotNull
    }
    // -- end of region smoke testing --

    @Test
    fun `attempting to get but not found`() {
        // -- mock --
        whenever(mockProductRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { productService.get(1L) }

        // -- verify --
        verify(mockProductRepository).findById(any<Long>())
    }

    @Test
    fun `get and found`() {
        val mockProduct = createMockProduct("Diamond Ring")
        // -- mock --
        whenever(mockProductRepository.findById(any<Long>())).thenReturn(Optional.of(mockProduct))

        // -- execute --
        val result = productService.get(1L)
        assertThat(result).usingRecursiveComparison().isEqualTo(mockProduct)

        // -- verify --
        verify(mockProductRepository).findById(any<Long>())
    }

    @Test
    fun `attempting to find all`() {
        val productNames = listOf("Diamond Ring", "Gold Necklace", "Silver Bracelet")
        val products = productNames.map { createMockProduct(it) }
        // -- mock --
        whenever(
            mockProductRepository.findAllCustom(
                anyOrNull(),
                anyOrNull(),
                any<BigDecimal>(),
                any<BigDecimal>(),
                any<Pageable>()
            )
        ).thenReturn(PageImpl(products))

        // -- execute --
        val result = productService.findAll(
            null,
            null,
            BigDecimal.ZERO,
            BigDecimal("999999999"),
            Pageable.unpaged()
        )

        assertThat(result).isNotEmpty

        // -- verify --
        verify(mockProductRepository).findAllCustom(
            anyOrNull(),
            anyOrNull(),
            any<BigDecimal>(),
            any<BigDecimal>(),
            any<Pageable>()
        )
    }

    @Test
    fun `find all with search filter by name`() {
        val products = listOf(createMockProduct("Diamond Ring"))
        // -- mock --
        whenever(
            mockProductRepository.findAllCustom(
                "Diamond",
                null,
                BigDecimal.ZERO,
                BigDecimal("999999999"),
                Pageable.unpaged()
            )
        ).thenReturn(PageImpl(products))

        // -- execute --
        val result = productService.findAll(
            "Diamond",
            null,
            BigDecimal.ZERO,
            BigDecimal("999999999"),
            Pageable.unpaged()
        )

        assertThat(result).hasSize(1)
        assertThat(result.content[0].name).contains("Diamond")

        // -- verify --
        verify(mockProductRepository).findAllCustom(
            "Diamond",
            null,
            BigDecimal.ZERO,
            BigDecimal("999999999"),
            Pageable.unpaged()
        )
    }

    @Test
    fun `find all with search filter by description`() {
        val products = listOf(createMockProduct("Gold Ring"))
        // -- mock --
        whenever(
            mockProductRepository.findAllCustom(
                "Test description",
                null,
                BigDecimal.ZERO,
                BigDecimal("999999999"),
                Pageable.unpaged()
            )
        ).thenReturn(PageImpl(products))

        // -- execute --
        val result = productService.findAll(
            "Test description",
            null,
            BigDecimal.ZERO,
            BigDecimal("999999999"),
            Pageable.unpaged()
        )

        assertThat(result).hasSize(1)

        // -- verify --
        verify(mockProductRepository).findAllCustom(
            "Test description",
            null,
            BigDecimal.ZERO,
            BigDecimal("999999999"),
            Pageable.unpaged()
        )
    }

    @Test
    fun `find all with search filter by specificationInHtml`() {
        val products = listOf(createMockProduct("Gold Ring"))
        // -- mock --
        whenever(
            mockProductRepository.findAllCustom(
                "Test specification",
                null,
                BigDecimal.ZERO,
                BigDecimal("999999999"),
                Pageable.unpaged()
            )
        ).thenReturn(PageImpl(products))

        // -- execute --
        val result = productService.findAll(
            "Test specification",
            null,
            BigDecimal.ZERO,
            BigDecimal("999999999"),
            Pageable.unpaged()
        )

        assertThat(result).hasSize(1)

        // -- verify --
        verify(mockProductRepository).findAllCustom(
            "Test specification",
            null,
            BigDecimal.ZERO,
            BigDecimal("999999999"),
            Pageable.unpaged()
        )
    }

    @Test
    fun `find all with category filter`() {
        val products = listOf(createMockProduct("Diamond Ring"), createMockProduct("Gold Ring"))
        // -- mock --
        whenever(
            mockProductRepository.findAllCustom(
                null,
                ProductCategory.RING,
                BigDecimal.ZERO,
                BigDecimal("999999999"),
                Pageable.unpaged()
            )
        ).thenReturn(PageImpl(products))

        // -- execute --
        val result = productService.findAll(
            null,
            ProductCategory.RING,
            BigDecimal.ZERO,
            BigDecimal("999999999"),
            Pageable.unpaged()
        )

        assertThat(result).hasSize(2)
        assertThat(result.content).allMatch { it.category == ProductCategory.RING }

        // -- verify --
        verify(mockProductRepository).findAllCustom(
            null,
            ProductCategory.RING,
            BigDecimal.ZERO,
            BigDecimal("999999999"),
            Pageable.unpaged()
        )
    }

    @Test
    fun `find all with price range filter`() {
        val products = listOf(createMockProduct("Mid-priced Ring"))
        // -- mock --
        whenever(
            mockProductRepository.findAllCustom(
                null,
                null,
                BigDecimal("1000"),
                BigDecimal("2000"),
                Pageable.unpaged()
            )
        ).thenReturn(PageImpl(products))

        // -- execute --
        val result = productService.findAll(
            null,
            null,
            BigDecimal("1000"),
            BigDecimal("2000"),
            Pageable.unpaged()
        )

        assertThat(result).hasSize(1)

        // -- verify --
        verify(mockProductRepository).findAllCustom(
            null,
            null,
            BigDecimal("1000"),
            BigDecimal("2000"),
            Pageable.unpaged()
        )
    }

    @Test
    fun `find all with multiple filters`() {
        val products = listOf(createMockProduct("Diamond Ring"))
        // -- mock --
        whenever(
            mockProductRepository.findAllCustom(
                "Diamond",
                ProductCategory.RING,
                BigDecimal("1000"),
                BigDecimal("2000"),
                Pageable.unpaged()
            )
        ).thenReturn(PageImpl(products))

        // -- execute --
        val result = productService.findAll(
            "Diamond",
            ProductCategory.RING,
            BigDecimal("1000"),
            BigDecimal("2000"),
            Pageable.unpaged()
        )

        assertThat(result).hasSize(1)
        assertThat(result.content[0].name).contains("Diamond")
        assertThat(result.content[0].category).isEqualTo(ProductCategory.RING)

        // -- verify --
        verify(mockProductRepository).findAllCustom(
            "Diamond",
            ProductCategory.RING,
            BigDecimal("1000"),
            BigDecimal("2000"),
            Pageable.unpaged()
        )
    }

    @Test
    fun `attempting to create and success`() = runTest {
        val mockMultipartFile = MockMultipartFile("image.jpg", ByteArray(1024))
        val mockProduct = createMockProduct("Diamond Ring")
        val mockProductMedia = ProductMedia("https://example.com/image.jpg", 0, mockProduct)
        val mediaKey = "products/image.jpg"
        val publicUrl = "https://example.com/image.jpg"
        val generatedSku = "RING_26_001"
        // -- mock --
        whenever(mockHtmlSanitizer.sanitize(any())).thenAnswer { it.arguments[0] }
        whenever(mockSkuGenerationService.generateSku(any<ProductCategory>())).thenReturn(generatedSku)
        whenever(mockProductRepository.save(any<Product>())).thenReturn(mockProduct)
        whenever(
            mockSupabaseService.uploadFile(
                any<String>(),
                any<String>(),
                any<MultipartFile>(),
                any<Boolean>()
            )
        ).thenReturn(mediaKey)
        whenever(mockSupabaseService.getPublicUrl(any<String>())).thenReturn(publicUrl)
        whenever(mockProductMediaRepository.save(any<ProductMedia>())).thenReturn(mockProductMedia)

        // -- execute --
        val result = productService.create(
            com.mondi.machine.backoffices.products.BackofficeProductRequest(
                name = "Diamond Ring",
                description = "Beautiful diamond ring",
                price = BigDecimal("1500.00"),
                currency = com.mondi.machine.utils.Currency.USD,
                specificationInHtml = "<p>14k gold</p>",
                discountPercentage = BigDecimal("10.00"),
                mediaFiles = listOf(mockMultipartFile),
                category = ProductCategory.RING,
                stock = 50
            )
        )
        assertThat(result).usingRecursiveComparison().ignoringFields("media").isEqualTo(mockProduct)

        // -- verify --
        verify(mockSkuGenerationService).generateSku(any<ProductCategory>())
        verify(mockProductRepository).save(any<Product>())
        verify(mockSupabaseService).uploadFile(any<String>(), any<String>(), any<MultipartFile>(), any<Boolean>())
        verify(mockSupabaseService).getPublicUrl(any<String>())
        verify(mockProductMediaRepository).save(any<ProductMedia>())
    }

    @Test
    fun `creating product generates unique SKU`() = runTest {
        val mockMultipartFile = MockMultipartFile("image.jpg", ByteArray(1024))
        val mockProduct = createMockProduct("Test Ring")
        val mockProductMedia = ProductMedia("https://example.com/image.jpg", 0, mockProduct)
        val mediaKey = "products/image.jpg"
        val publicUrl = "https://example.com/image.jpg"
        val generatedSku = "RING_26_042"
        // -- mock --
        whenever(mockHtmlSanitizer.sanitize(any())).thenAnswer { it.arguments[0] }
        whenever(mockSkuGenerationService.generateSku(ProductCategory.RING)).thenReturn(generatedSku)
        whenever(mockProductRepository.save(any<Product>())).thenReturn(mockProduct)
        whenever(
            mockSupabaseService.uploadFile(
                any<String>(),
                any<String>(),
                any<MultipartFile>(),
                any<Boolean>()
            )
        ).thenReturn(mediaKey)
        whenever(mockSupabaseService.getPublicUrl(any<String>())).thenReturn(publicUrl)
        whenever(mockProductMediaRepository.save(any<ProductMedia>())).thenReturn(mockProductMedia)

        // -- execute --
        productService.create(
            com.mondi.machine.backoffices.products.BackofficeProductRequest(
                name = "Test Ring",
                description = "Test description",
                price = BigDecimal("1500.00"),
                currency = com.mondi.machine.utils.Currency.USD,
                specificationInHtml = "<p>14k gold</p>",
                discountPercentage = BigDecimal("10.00"),
                mediaFiles = listOf(mockMultipartFile),
                category = ProductCategory.RING,
                stock = 50
            )
        )

        // -- verify SKU generation was called with correct category --
        verify(mockSkuGenerationService).generateSku(ProductCategory.RING)
    }

    @Test
    fun `creating product sanitizes HTML specification`() = runTest {
        val mockMultipartFile = MockMultipartFile("image.jpg", ByteArray(1024))
        val mockProduct = createMockProduct("Test Ring")
        val mockProductMedia = ProductMedia("https://example.com/image.jpg", 0, mockProduct)
        val mediaKey = "products/image.jpg"
        val publicUrl = "https://example.com/image.jpg"
        val generatedSku = "RING_26_001"
        val unsafeHtml = "<p>Safe content</p><script>alert('XSS')</script>"
        val sanitizedHtml = "<p>Safe content</p>"

        // -- mock --
        whenever(mockHtmlSanitizer.sanitize(unsafeHtml)).thenReturn(sanitizedHtml)
        whenever(mockSkuGenerationService.generateSku(any<ProductCategory>())).thenReturn(generatedSku)
        whenever(mockProductRepository.save(any<Product>())).thenAnswer { invocation ->
            val product = invocation.arguments[0] as Product
            // Verify HTML was sanitized
            assertThat(product.specificationInHtml).isEqualTo(sanitizedHtml)
            assertThat(product.specificationInHtml).doesNotContain("script")
            mockProduct
        }
        whenever(
            mockSupabaseService.uploadFile(
                any<String>(),
                any<String>(),
                any<MultipartFile>(),
                any<Boolean>()
            )
        ).thenReturn(mediaKey)
        whenever(mockSupabaseService.getPublicUrl(any<String>())).thenReturn(publicUrl)
        whenever(mockProductMediaRepository.save(any<ProductMedia>())).thenReturn(mockProductMedia)

        // -- execute --
        productService.create(
            com.mondi.machine.backoffices.products.BackofficeProductRequest(
                name = "Test Ring",
                description = "Test description",
                price = BigDecimal("1500.00"),
                currency = com.mondi.machine.utils.Currency.USD,
                specificationInHtml = unsafeHtml,
                discountPercentage = BigDecimal("10.00"),
                mediaFiles = listOf(mockMultipartFile),
                category = ProductCategory.RING,
                stock = 50
            )
        )

        // -- verify HTML sanitization was called --
        verify(mockHtmlSanitizer).sanitize(unsafeHtml)
        verify(mockProductRepository).save(any<Product>())
    }

    @Test
    fun `created product has ACTIVE status by default`() = runTest {
        val mockMultipartFile = MockMultipartFile("image.jpg", ByteArray(1024))
        val mockProduct = createMockProduct("Diamond Ring")
        val mockProductMedia = ProductMedia("https://example.com/image.jpg", 0, mockProduct)
        val mediaKey = "products/image.jpg"
        val publicUrl = "https://example.com/image.jpg"
        val generatedSku = "RING_26_001"
        // -- mock --
        whenever(mockSkuGenerationService.generateSku(any<ProductCategory>())).thenReturn(generatedSku)
        whenever(mockProductRepository.save(any<Product>())).thenAnswer { invocation ->
            val product = invocation.arguments[0] as Product
            assertThat(product.status).isEqualTo(ProductStatus.ACTIVE)
            mockProduct
        }
        whenever(
            mockSupabaseService.uploadFile(
                any<String>(),
                any<String>(),
                any<MultipartFile>(),
                any<Boolean>()
            )
        ).thenReturn(mediaKey)
        whenever(mockSupabaseService.getPublicUrl(any<String>())).thenReturn(publicUrl)
        whenever(mockProductMediaRepository.save(any<ProductMedia>())).thenReturn(mockProductMedia)

        // -- execute --
        val result = productService.create(
            com.mondi.machine.backoffices.products.BackofficeProductRequest(
                name = "Diamond Ring",
                description = "Beautiful diamond ring",
                price = BigDecimal("1500.00"),
                currency = com.mondi.machine.utils.Currency.USD,
                specificationInHtml = "<p>14k gold</p>",
                discountPercentage = BigDecimal("10.00"),
                mediaFiles = listOf(mockMultipartFile),
                category = ProductCategory.RING,
                stock = 50
            )
        )

        assertThat(result.status).isEqualTo(ProductStatus.ACTIVE)
    }

    @Test
    fun `attempting to delete and success`() {
        val mockProduct = createMockProduct("Diamond Ring")
        // -- mock --
        whenever(mockProductRepository.findById(any<Long>())).thenReturn(Optional.of(mockProduct))

        // -- execute --
        productService.delete(1L)

        // -- verify --
        verify(mockProductRepository).findById(any<Long>())
        verify(mockProductRepository).delete(any<Product>())
    }

    @Test
    fun `update product with media management succeeds`() = runTest {
        val mockProduct = createMockProduct("Diamond Ring")
        val mockNewFile = MockMultipartFile("image.jpg", ByteArray(1024))
        val existingMediaUrls = listOf("https://example.com/image1.jpg")
        val mediaKey = "products/new-image.jpg"
        val publicUrl = "https://example.com/new-image.jpg"

        // -- mock --
        whenever(mockHtmlSanitizer.sanitize(any())).thenAnswer { it.arguments[0] }
        whenever(mockProductRepository.findById(any<Long>())).thenReturn(Optional.of(mockProduct))
        whenever(mockProductRepository.save(any<Product>())).thenReturn(mockProduct)
        whenever(mockProductMediaRepository.save(any<ProductMedia>())).thenAnswer { it.arguments[0] as ProductMedia }
        whenever(
            mockSupabaseService.uploadFile(
                any<String>(),
                any<String>(),
                any<MultipartFile>(),
                any<Boolean>()
            )
        ).thenReturn(mediaKey)
        whenever(mockSupabaseService.getPublicUrl(any<String>())).thenReturn(publicUrl)

        // -- execute --
        val result = productService.updateWithMediaManagement(
            id = 1L,
            name = "Updated Ring",
            description = "Updated description",
            price = BigDecimal("2000.00"),
            currency = "USD",
            specificationInHtml = "<p>18k gold</p>",
            discountPercentage = BigDecimal("15.00"),
            category = ProductCategory.RING,
            stock = 30,
            existingMediaUrls = existingMediaUrls,
            newMediaFiles = listOf(mockNewFile),
            status = ProductStatus.ACTIVE
        )

        assertThat(result.name).isEqualTo("Updated Ring")
        assertThat(result.price).isEqualTo(BigDecimal("2000.00"))
        assertThat(result.stock).isEqualTo(30)

        // Verify new media was uploaded
        verify(mockSupabaseService).uploadFile(any<String>(), any<String>(), any<MultipartFile>(), any<Boolean>())
        verify(mockSupabaseService).getPublicUrl(any<String>())

        // -- verify --
        verify(mockProductRepository).findById(any<Long>())
        verify(mockProductRepository).save(any<Product>())
    }

    private fun createMockProduct(name: String): Product {
        return Product(
            name = name,
            description = "Test description",
            price = BigDecimal("1500.00"),
            currency = "USD",
            specificationInHtml = "<p>Test specification</p>",
            discountPercentage = BigDecimal("10.00"),
            category = ProductCategory.RING,
            stock = 50,
            sku = "RING_26_001",
            status = ProductStatus.ACTIVE
        ).apply {
            this.id = 1L
        }
    }
}
