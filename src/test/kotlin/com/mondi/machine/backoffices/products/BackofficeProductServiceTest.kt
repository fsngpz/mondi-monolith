package com.mondi.machine.backoffices.products

import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductCategory
import com.mondi.machine.products.ProductService
import com.mondi.machine.products.ProductStatus
import com.mondi.machine.utils.Currency
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal

/**
 * The test class for [BackofficeProductService].
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@SpringBootTest(classes = [BackofficeProductService::class])
@ActiveProfiles("test")
internal class BackofficeProductServiceTest(
    @Autowired private val backofficeProductService: BackofficeProductService
) {
    @MockitoBean
    private lateinit var mockProductService: ProductService

    @Test
    fun `dependencies are not null`() {
        assertThat(backofficeProductService).isNotNull
        assertThat(mockProductService).isNotNull
    }

    @Test
    fun `create product success`() = runTest {
        val mockMultipartFile = MockMultipartFile("image", ByteArray(1024))
        val mockRequest = BackofficeProductRequest(
            name = "Diamond Ring",
            description = "Beautiful diamond ring",
            price = BigDecimal("1500.00"),
            currency = Currency.USD,
            specificationInHtml = "<p>14k gold</p>",
            discountPercentage = BigDecimal("10.00"),
            mediaFiles = listOf(mockMultipartFile),
            category = ProductCategory.RING,
            stock = 50
        )
        val mockProduct = createMockProduct("Diamond Ring")
        // -- mock --
        whenever(
            mockProductService.create(any<BackofficeProductRequest>())
        ).thenReturn(mockProduct)

        // -- execute --
        val result = backofficeProductService.create(mockRequest)
        assertThat(result.name).isEqualTo(mockProduct.name)
        assertThat(result.price).isEqualTo(mockProduct.price)
        assertThat(result.category).isEqualTo(mockProduct.category)
        assertThat(result.stock).isEqualTo(mockProduct.stock)
        assertThat(result.sku).isNotBlank()
        assertThat(result.status).isEqualTo(ProductStatus.ACTIVE)

        // -- verify --
        verify(mockProductService).create(any<BackofficeProductRequest>())
    }

    @Test
    fun `update product success`() = runTest {
        val mockMultipartFile = MockMultipartFile("image", ByteArray(1024))
        val mockRequest = BackofficeProductRequest(
            name = "Updated Ring",
            description = "Updated description",
            price = BigDecimal("2000.00"),
            currency = Currency.USD,
            specificationInHtml = "<p>18k gold</p>",
            discountPercentage = BigDecimal("15.00"),
            mediaFiles = listOf(mockMultipartFile),
            category = ProductCategory.RING,
            stock = 30
        )
        val mockProduct = createMockProduct("Updated Ring")
        // -- mock --
        whenever(
            mockProductService.update(any<Long>(), any())
        ).thenReturn(mockProduct)

        // -- execute --
        val result = backofficeProductService.update(1L, mockRequest)
        assertThat(result.name).isEqualTo(mockProduct.name)
        assertThat(result.price).isEqualTo(mockProduct.price)
        assertThat(result.category).isEqualTo(mockProduct.category)
        assertThat(result.stock).isEqualTo(mockProduct.stock)

        // -- verify --
        verify(mockProductService).update(any<Long>(), any())
    }

    @Test
    fun `delete product success`() {
        // -- execute --
        backofficeProductService.delete(1L)

        // -- verify --
        verify(mockProductService).delete(any<Long>())
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
