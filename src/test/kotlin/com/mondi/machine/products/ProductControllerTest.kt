package com.mondi.machine.products

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

/**
 * The test class for [ProductController].
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-23
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
internal class ProductControllerTest(@Autowired private val mockMvc: MockMvc) {

    @MockitoBean
    private lateinit var mockProductService: ProductService

    @Test
    fun `get product by id returns product details`() {
        val mockProduct = createMockProduct("Diamond Ring")
        // -- mock --
        whenever(mockProductService.get(any<Long>())).thenReturn(mockProduct)

        // -- execute --
        mockMvc.perform(get("/v1/products/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Diamond Ring"))
            .andExpect(jsonPath("$.price").value(1500.00))
            .andExpect(jsonPath("$.category").value("RING"))
            .andExpect(jsonPath("$.sku").value("RING_26_001"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        // -- verify --
        verify(mockProductService).get(1L)
    }

    @Test
    fun `findAll without filters returns all products`() {
        val products = listOf(
            createMockProduct("Diamond Ring"),
            createMockProduct("Gold Necklace"),
            createMockProduct("Silver Bracelet")
        )
        val productResponses = products.map { it.toResponse() }
        val pageable = PageRequest.of(0, 10)
        // -- mock --
        whenever(
            mockProductService.findAll(
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal("10"),
                pageable
            )
        ).thenReturn(PageImpl(productResponses, pageable, productResponses.size.toLong()))

        // -- execute --
        mockMvc.perform(get("/v1/products?page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.content[0].name").value("Diamond Ring"))
            .andExpect(jsonPath("$.content[1].name").value("Gold Necklace"))
            .andExpect(jsonPath("$.content[2].name").value("Silver Bracelet"))

        // -- verify --
        verify(mockProductService).findAll(null, null, BigDecimal.ZERO, BigDecimal("10"), pageable)
    }

    @Test
    fun `findAll with search parameter filters by name`() {
        val products = listOf(createMockProduct("Diamond Ring"))
        val productResponses = products.map { it.toResponse() }
        val pageable = PageRequest.of(0, 10)
        // -- mock --
        whenever(
            mockProductService.findAll(
                "Diamond",
                null,
                BigDecimal.ZERO,
                BigDecimal("10"),
                pageable
            )
        ).thenReturn(PageImpl(productResponses, pageable, productResponses.size.toLong()))

        // -- execute --
        mockMvc.perform(get("/v1/products?search=Diamond&page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Diamond Ring"))

        // -- verify --
        verify(mockProductService).findAll("Diamond", null, BigDecimal.ZERO, BigDecimal("10"), pageable)
    }

    @Test
    fun `findAll with search parameter filters by description`() {
        val products = listOf(createMockProduct("Gold Ring"))
        val productResponses = products.map { it.toResponse() }
        val pageable = PageRequest.of(0, 10)
        // -- mock --
        whenever(
            mockProductService.findAll(
                "Beautiful",
                null,
                BigDecimal.ZERO,
                BigDecimal("10"),
                pageable
            )
        ).thenReturn(PageImpl(productResponses, pageable, productResponses.size.toLong()))

        // -- execute --
        mockMvc.perform(get("/v1/products?search=Beautiful&page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))

        // -- verify --
        verify(mockProductService).findAll("Beautiful", null, BigDecimal.ZERO, BigDecimal("10"), pageable)
    }

    @Test
    fun `findAll with search parameter filters by specificationInHtml`() {
        val products = listOf(createMockProduct("Gold Ring"))
        val productResponses = products.map { it.toResponse() }
        val pageable = PageRequest.of(0, 10)
        // -- mock --
        whenever(
            mockProductService.findAll(
                "14k gold",
                null,
                BigDecimal.ZERO,
                BigDecimal("10"),
                pageable
            )
        ).thenReturn(PageImpl(productResponses, pageable, productResponses.size.toLong()))

        // -- execute --
        mockMvc.perform(get("/v1/products?search=14k gold&page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))

        // -- verify --
        verify(mockProductService).findAll("14k gold", null, BigDecimal.ZERO, BigDecimal("10"), pageable)
    }

    @Test
    fun `findAll with category filter returns products of specific category`() {
        val products = listOf(
            createMockProduct("Diamond Ring"),
            createMockProduct("Gold Ring")
        )
        val productResponses = products.map { it.toResponse() }
        val pageable = PageRequest.of(0, 10)
        // -- mock --
        whenever(
            mockProductService.findAll(
                null,
                ProductCategory.RING,
                BigDecimal.ZERO,
                BigDecimal("10"),
                pageable
            )
        ).thenReturn(PageImpl(productResponses, pageable, productResponses.size.toLong()))

        // -- execute --
        mockMvc.perform(get("/v1/products?category=RING&page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].category").value("RING"))
            .andExpect(jsonPath("$.content[1].category").value("RING"))

        // -- verify --
        verify(mockProductService).findAll(null, ProductCategory.RING, BigDecimal.ZERO, BigDecimal("10"), pageable)
    }

    @Test
    fun `findAll with price range filters products correctly`() {
        val products = listOf(createMockProduct("Mid-priced Ring"))
        val productResponses = products.map { it.toResponse() }
        val pageable = PageRequest.of(0, 10)
        // -- mock --
        whenever(
            mockProductService.findAll(
                null,
                null,
                BigDecimal("1000"),
                BigDecimal("2000"),
                pageable
            )
        ).thenReturn(PageImpl(productResponses, pageable, productResponses.size.toLong()))

        // -- execute --
        mockMvc.perform(get("/v1/products?minPrice=1000&maxPrice=2000&page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))

        // -- verify --
        verify(mockProductService).findAll(
            null,
            null,
            BigDecimal("1000"),
            BigDecimal("2000"),
            pageable
        )
    }

    @Test
    fun `findAll with multiple filters applies all filters correctly`() {
        val products = listOf(createMockProduct("Diamond Ring"))
        val productResponses = products.map { it.toResponse() }
        val pageable = PageRequest.of(0, 10)
        // -- mock --
        whenever(
            mockProductService.findAll(
                "Diamond",
                ProductCategory.RING,
                BigDecimal("1000"),
                BigDecimal("2000"),
                pageable
            )
        ).thenReturn(PageImpl(productResponses, pageable, productResponses.size.toLong()))

        // -- execute --
        mockMvc.perform(
            get("/v1/products?search=Diamond&category=RING&minPrice=1000&maxPrice=2000&page=0&size=10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Diamond Ring"))
            .andExpect(jsonPath("$.content[0].category").value("RING"))

        // -- verify --
        verify(mockProductService).findAll(
            "Diamond",
            ProductCategory.RING,
            BigDecimal("1000"),
            BigDecimal("2000"),
            pageable
        )
    }

    @Test
    fun `findAll returns empty list when no products match filters`() {
        val pageable = PageRequest.of(0, 10)
        // -- mock --
        whenever(
            mockProductService.findAll(
                "NonExistent",
                null,
                BigDecimal.ZERO,
                BigDecimal("10"),
                pageable
            )
        ).thenReturn(PageImpl(emptyList(), pageable, 0))

        // -- execute --
        mockMvc.perform(get("/v1/products?search=NonExistent&page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))

        // -- verify --
        verify(mockProductService).findAll("NonExistent", null, BigDecimal.ZERO, BigDecimal("10"), pageable)
    }

    @Test
    fun `findAll with pagination returns correct page`() {
        val products = listOf(
            createMockProduct("Product 11"),
            createMockProduct("Product 12")
        )
        val productResponses = products.map { it.toResponse() }
        val pageable = PageRequest.of(1, 10)
        // -- mock --
        whenever(
            mockProductService.findAll(
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal("10"),
                pageable
            )
        ).thenReturn(PageImpl(productResponses, pageable, 12))

        // -- execute --
        mockMvc.perform(get("/v1/products?page=1&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.totalElements").value(12))

        // -- verify --
        verify(mockProductService).findAll(null, null, BigDecimal.ZERO, BigDecimal("10"), pageable)
    }

    private fun createMockProduct(name: String): Product {
        return Product(
            name = name,
            description = "Beautiful ${name.lowercase()}",
            price = BigDecimal("1500.00"),
            currency = "USD",
            specificationInHtml = "<p>14k gold specification</p>",
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
