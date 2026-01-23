package com.mondi.machine.products

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Year

/**
 * The test class for [SkuGenerationService].
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-23
 */
@SpringBootTest(classes = [SkuGenerationService::class])
@ActiveProfiles("test")
internal class SkuGenerationServiceTest(@Autowired private val skuGenerationService: SkuGenerationService) {

    @MockitoBean
    lateinit var mockProductRepository: ProductRepository

    @Test
    fun `dependencies are not null`() {
        assertThat(skuGenerationService).isNotNull
        assertThat(mockProductRepository).isNotNull
    }

    @Test
    fun `generate SKU with correct format for RING`() {
        // -- mock --
        val skuPrefix = "RING_${Year.now().value % 100}_"
        whenever(mockProductRepository.countBySkuPrefix(skuPrefix)).thenReturn(0L)

        // -- execute --
        val sku = skuGenerationService.generateSku(ProductCategory.RING)

        // -- assert --
        assertThat(sku).startsWith("RING_")
        assertThat(sku).matches("RING_\\d{2}_\\d{3}")
        assertThat(sku).endsWith("_001")
    }

    @Test
    fun `generate SKU with correct format for EARRING`() {
        // -- mock --
        val skuPrefix = "EARRING_${Year.now().value % 100}_"
        whenever(mockProductRepository.countBySkuPrefix(skuPrefix)).thenReturn(0L)

        // -- execute --
        val sku = skuGenerationService.generateSku(ProductCategory.EARRING)

        // -- assert --
        assertThat(sku).startsWith("EARRING_")
        assertThat(sku).matches("EARRING_\\d{2}_\\d{3}")
        assertThat(sku).endsWith("_001")
    }

    @Test
    fun `generate SKU with correct format for NECKLACE`() {
        // -- mock --
        val skuPrefix = "NECKLACE_${Year.now().value % 100}_"
        whenever(mockProductRepository.countBySkuPrefix(skuPrefix)).thenReturn(0L)

        // -- execute --
        val sku = skuGenerationService.generateSku(ProductCategory.NECKLACE)

        // -- assert --
        assertThat(sku).startsWith("NECKLACE_")
        assertThat(sku).matches("NECKLACE_\\d{2}_\\d{3}")
        assertThat(sku).endsWith("_001")
    }

    @Test
    fun `generate SKU increments correctly`() {
        // -- mock: 41 existing products with RING_26_ prefix --
        val skuPrefix = "RING_${Year.now().value % 100}_"
        whenever(mockProductRepository.countBySkuPrefix(skuPrefix)).thenReturn(41L)

        // -- execute --
        val sku = skuGenerationService.generateSku(ProductCategory.RING)

        // -- assert --
        assertThat(sku).endsWith("_042")
    }

    @Test
    fun `generate SKU pads incrementing number with zeros`() {
        // -- mock: 4 existing products --
        val skuPrefix = "BRACELET_${Year.now().value % 100}_"
        whenever(mockProductRepository.countBySkuPrefix(skuPrefix)).thenReturn(4L)

        // -- execute --
        val sku = skuGenerationService.generateSku(ProductCategory.BRACELET)

        // -- assert --
        assertThat(sku).endsWith("_005")
        assertThat(sku).matches("BRACELET_\\d{2}_\\d{3}")
    }

    @Test
    fun `generate SKU handles triple digit increment`() {
        // -- mock: 99 existing products --
        val skuPrefix = "PENDANT_${Year.now().value % 100}_"
        whenever(mockProductRepository.countBySkuPrefix(skuPrefix)).thenReturn(99L)

        // -- execute --
        val sku = skuGenerationService.generateSku(ProductCategory.PENDANT)

        // -- assert --
        assertThat(sku).endsWith("_100")
        assertThat(sku).matches("PENDANT_\\d{2}_\\d{3}")
    }

    @Test
    fun `generate SKU uses correct year`() {
        // -- mock --
        val currentYear = Year.now().value % 100
        val yearStr = currentYear.toString().padStart(2, '0')
        val skuPrefix = "OTHER_${yearStr}_"
        whenever(mockProductRepository.countBySkuPrefix(skuPrefix)).thenReturn(0L)

        // -- execute --
        val sku = skuGenerationService.generateSku(ProductCategory.OTHER)

        // -- assert --
        assertThat(sku).contains("_${yearStr}_")
    }

    @Test
    fun `different categories generate different SKU prefixes`() {
        // -- mock --
        val year = Year.now().value % 100
        whenever(mockProductRepository.countBySkuPrefix("RING_${year}_")).thenReturn(0L)
        whenever(mockProductRepository.countBySkuPrefix("EARRING_${year}_")).thenReturn(0L)

        // -- execute --
        val ringSku = skuGenerationService.generateSku(ProductCategory.RING)
        val earringSku = skuGenerationService.generateSku(ProductCategory.EARRING)

        // -- assert --
        assertThat(ringSku).startsWith("RING_")
        assertThat(earringSku).startsWith("EARRING_")
        assertThat(ringSku).isNotEqualTo(earringSku)
    }
}
