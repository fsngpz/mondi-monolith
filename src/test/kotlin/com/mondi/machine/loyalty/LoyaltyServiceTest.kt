package com.mondi.machine.loyalty

import com.mondi.machine.orders.OrderTestFixtures
import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * The test class for [LoyaltyService].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@SpringBootTest(classes = [LoyaltyService::class])
internal class LoyaltyServiceTest(@Autowired private val service: LoyaltyService) {

    @MockitoBean
    lateinit var mockRepository: LoyaltyPointRepository

    @ParameterizedTest
    @CsvSource("0, 0", "9999, 0", "10000, 1", "25999999, 2599", "1500000.75, 150")
    fun `one point per full 10,000 IDR`(amount: String, points: Int) {
        assertThat(LoyaltyService.pointsFor(BigDecimal(amount))).isEqualTo(points)
    }

    @ParameterizedTest
    @CsvSource("0, BRONZE", "999, BRONZE", "1000, SILVER", "2999, SILVER", "3000, GOLD", "10000, DIAMOND", "250000, DIAMOND")
    fun `tier follows the points thresholds`(points: Int, tier: LoyaltyTier) {
        assertThat(LoyaltyTier.forPoints(points)).isEqualTo(tier)
    }

    @Test
    fun `balance shows the tier and points to the next tier`() {
        whenever(mockRepository.sumPointsByProfileId(1L)).thenReturn(1250)

        val result = service.getBalance(1L)

        assertThat(result.tier).isEqualTo(LoyaltyTier.SILVER)
        assertThat(result.nextTier).isEqualTo(LoyaltyTier.GOLD)
        assertThat(result.pointsToNextTier).isEqualTo(1750)
    }

    @Test
    fun `diamond members have no next tier`() {
        whenever(mockRepository.sumPointsByProfileId(1L)).thenReturn(12000)

        val result = service.getBalance(1L)

        assertThat(result.tier).isEqualTo(LoyaltyTier.DIAMOND)
        assertThat(result.nextTier).isNull()
        assertThat(result.pointsToNextTier).isNull()
    }

    @Test
    fun `a delivered order earns points once`() {
        val profile = OrderTestFixtures.profile()
        whenever(mockRepository.existsByOrderIdAndTransactionType(10L, LoyaltyTransactionType.EARNED)).thenReturn(false)

        val earned = service.earnPoints(profile, 10L, BigDecimal("2600000"))

        assertThat(earned).isEqualTo(260)
        val saved = argumentCaptor<LoyaltyPoint>()
        verify(mockRepository).save(saved.capture())
        assertThat(saved.firstValue.points).isEqualTo(260)
        assertThat(saved.firstValue.orderId).isEqualTo(10L)
        assertThat(saved.firstValue.transactionType).isEqualTo(LoyaltyTransactionType.EARNED)
    }

    @Test
    fun `an order that already earned points earns nothing more`() {
        whenever(mockRepository.existsByOrderIdAndTransactionType(10L, LoyaltyTransactionType.EARNED)).thenReturn(true)

        assertThat(service.earnPoints(OrderTestFixtures.profile(), 10L, BigDecimal("2600000"))).isZero()
        verify(mockRepository, never()).save(any<LoyaltyPoint>())
    }

    @Test
    fun `an order below 10,000 IDR earns nothing and saves nothing`() {
        assertThat(service.earnPoints(OrderTestFixtures.profile(), 10L, BigDecimal("9999"))).isZero()
        verify(mockRepository, never()).save(any<LoyaltyPoint>())
    }
}
