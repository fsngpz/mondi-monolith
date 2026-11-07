package com.mondi.machine.configs

import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductCategory
import com.mondi.machine.products.ProductService
import com.mondi.machine.products.ProductStatus
import com.mondi.machine.reviews.ProductReviewsResponse
import com.mondi.machine.reviews.ReviewService
import com.mondi.machine.utils.Currency
import java.math.BigDecimal
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Checks the URL access rules of [WebSecurityConfig] with the security filters switched on,
 * as an anonymous (not signed in) caller.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
internal class WebSecurityConfigTest(@Autowired private val mockMvc: MockMvc) {

    @MockitoBean
    private lateinit var mockProductService: ProductService

    @MockitoBean
    private lateinit var mockReviewService: ReviewService

    @Test
    fun `anonymous user can read a single product`() {
        whenever(mockProductService.get(21L)).thenReturn(product())

        // -- the storefront product page calls this without a token --
        mockMvc.perform(get("/v1/products/21"))
            .andExpect(status().isOk)
    }

    @Test
    fun `anonymous user can list products`() {
        whenever(
            mockProductService.findAll(
                anyOrNull(), anyOrNull(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any()
            )
        ).thenReturn(Page.empty())

        // -- execute --
        mockMvc.perform(get("/v1/products"))
            .andExpect(status().isOk)
    }

    @Test
    fun `anonymous user cannot change products through the public path`() {
        // -- only GET is public under /v1/products --
        mockMvc.perform(delete("/v1/products/21"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `anonymous user can read product reviews`() {
        whenever(mockReviewService.getByProduct(any(), any()))
            .thenReturn(ProductReviewsResponse(null, 0, Page.empty()))

        // -- the product page shows reviews to visitors --
        mockMvc.perform(get("/v1/products/21/reviews"))
            .andExpect(status().isOk)
    }

    @Test
    fun `anonymous user cannot write a review`() {
        mockMvc.perform(post("/v1/reviews").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `anonymous user cannot read the account`() {
        mockMvc.perform(get("/v1/account/profiles"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `anonymous user cannot use the backoffice`() {
        mockMvc.perform(get("/v1/backoffice/products"))
            .andExpect(status().isUnauthorized)
    }

    private fun product(): Product {
        return Product(
            name = "Tennis Bracelet",
            description = null,
            currency = Currency.IDR,
            specificationInHtml = null,
            price = BigDecimal("1500000"),
            category = ProductCategory.BRACELET,
            stock = 3,
            sku = "BRACELET_26_021",
            status = ProductStatus.ACTIVE
        ).apply { this.id = 21L }
    }
}
