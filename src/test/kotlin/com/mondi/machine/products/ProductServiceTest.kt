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
    // -- end of region mock --

    // -- region of smoke testing --
    @Test
    fun `dependencies are not null`() {
        assertThat(productService).isNotNull
        assertThat(mockSupabaseService).isNotNull
        assertThat(mockProductRepository).isNotNull
        assertThat(mockProductMediaRepository).isNotNull
        assertThat(mockSkuGenerationService).isNotNull
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
    fun `attempting to create and success`() = runTest {
        val mockMultipartFile = MockMultipartFile("image.jpg", ByteArray(1024))
        val mockProduct = createMockProduct("Diamond Ring")
        val mockProductMedia = ProductMedia("https://example.com/image.jpg", 0, mockProduct)
        val mediaKey = "products/image.jpg"
        val publicUrl = "https://example.com/image.jpg"
        val generatedSku = "RING_26_001"
        // -- mock --
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
    fun `attempting to update and success`() = runTest {
        val mockMultipartFile = MockMultipartFile("image.jpg", ByteArray(1024))
        val mockProduct = createMockProduct("Updated Ring")
        val mockProductMedia = ProductMedia("https://example.com/image.jpg", 0, mockProduct)
        val mockRequest = ProductRequest(
            name = "Updated Ring",
            description = "Updated description",
            price = BigDecimal("2000.00"),
            currency = "USD",
            specificationInHtml = "<p>18k gold</p>",
            discountPercentage = BigDecimal("15.00"),
            mediaFiles = listOf(mockMultipartFile),
            category = ProductCategory.RING,
            stock = 30
        )
        val mediaKey = "products/image.jpg"
        val publicUrl = "https://example.com/image.jpg"
        // -- mock --
        whenever(mockProductRepository.findById(any<Long>())).thenReturn(Optional.of(mockProduct))
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
        val result = productService.update(1L, mockRequest)
        assertThat(result.name).isEqualTo(mockRequest.name)
        assertThat(result.price).isEqualTo(mockRequest.price)

        // -- verify --
        verify(mockProductRepository).findById(any<Long>())
        verify(mockProductMediaRepository).deleteByProductId(any<Long>())
        verify(mockSupabaseService).uploadFile(any<String>(), any<String>(), any<MultipartFile>(), any<Boolean>())
        verify(mockSupabaseService).getPublicUrl(any<String>())
        verify(mockProductMediaRepository).save(any<ProductMedia>())
        verify(mockProductRepository).save(any<Product>())
    }

    @Test
    fun `attempting to update but no product was found`() = runTest {
        val mockMultipartFile = MockMultipartFile("image.jpg", ByteArray(1024))
        val mockRequest = ProductRequest(
            name = "Updated Ring",
            description = "Updated description",
            price = BigDecimal("2000.00"),
            currency = "USD",
            specificationInHtml = "<p>18k gold</p>",
            discountPercentage = BigDecimal("15.00"),
            mediaFiles = listOf(mockMultipartFile),
            category = ProductCategory.RING,
            stock = 30
        )
        // -- mock --
        whenever(mockProductRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { productService.update(1L, mockRequest) }

        // -- verify --
        verify(mockProductRepository).findById(any<Long>())
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
