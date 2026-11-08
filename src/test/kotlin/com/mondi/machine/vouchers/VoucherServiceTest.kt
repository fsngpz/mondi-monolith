package com.mondi.machine.vouchers

import java.math.BigDecimal
import java.time.OffsetDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.Optional

/**
 * The test class for [VoucherService].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@SpringBootTest(classes = [VoucherService::class])
internal class VoucherServiceTest(@Autowired private val service: VoucherService) {

    @MockitoBean
    lateinit var mockRepository: VoucherRepository

    private val now = OffsetDateTime.now()

    private fun voucher(
        type: VoucherType = VoucherType.PERCENTAGE,
        value: String = "10",
        maxDiscount: String? = null,
        minOrder: String = "0",
        maxUsage: Int? = null,
        used: Int = 0,
        active: Boolean = true,
        start: OffsetDateTime = now.minusDays(1),
        end: OffsetDateTime = now.plusDays(30)
    ) = Voucher(
        code = "WELCOME10",
        description = null,
        type = type,
        value = BigDecimal(value),
        maxDiscountAmount = maxDiscount?.let { BigDecimal(it) },
        minOrderAmount = BigDecimal(minOrder),
        maxUsage = maxUsage,
        startDate = start,
        endDate = end,
        isActive = active
    ).apply {
        id = 1L
        currentUsage = used
    }

    private fun request(
        code: String = "summer-25",
        type: VoucherType = VoucherType.PERCENTAGE,
        value: String = "25",
        maxUsage: Int? = 100,
        start: OffsetDateTime = now,
        end: OffsetDateTime = now.plusDays(10)
    ) = VoucherRequest(
        code = code, type = type, value = BigDecimal(value), maxUsage = maxUsage, startDate = start, endDate = end
    )

    // -- region of validate --
    @Test
    fun `a valid percentage voucher returns the discount`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(voucher())

        // -- execute: the code is matched ignoring case and spaces --
        val result = service.validate("  welcome10 ", BigDecimal("2500000"))

        // -- verify --
        assertThat(result.valid).isTrue()
        assertThat(result.discountAmount).isEqualByComparingTo("250000")
        assertThat(result.code).isEqualTo("WELCOME10")
    }

    @Test
    fun `an unknown code is not valid`() {
        val result = service.validate("NOPE", BigDecimal("100000"))

        assertThat(result.valid).isFalse()
        assertThat(result.message).contains("does not exist")
        assertThat(result.discountAmount).isEqualByComparingTo("0")
    }

    @Test
    fun `an expired voucher is not valid`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10"))
            .thenReturn(voucher(start = now.minusDays(30), end = now.minusDays(1)))

        val result = service.validate("WELCOME10", BigDecimal("100000"))

        assertThat(result.valid).isFalse()
        assertThat(result.message).contains("expired")
    }

    @Test
    fun `a voucher that has not started is not valid`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(voucher(start = now.plusDays(1)))

        assertThat(service.validate("WELCOME10", BigDecimal("100000")).message).contains("not valid yet")
    }

    @Test
    fun `an inactive voucher is not valid`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(voucher(active = false))

        assertThat(service.validate("WELCOME10", BigDecimal("100000")).message).contains("not active")
    }

    @Test
    fun `a fully used voucher is not valid`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(voucher(maxUsage = 5, used = 5))

        assertThat(service.validate("WELCOME10", BigDecimal("100000")).message).contains("fully used")
    }

    @Test
    fun `a subtotal below the minimum order is not valid`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(voucher(minOrder = "1000000"))

        val result = service.validate("WELCOME10", BigDecimal("999999"))

        assertThat(result.valid).isFalse()
        assertThat(result.message).contains("minimum order of IDR 1000000")
    }
    // -- end of region validate --

    // -- region of discount --
    @Test
    fun `percentage discount is capped by the max discount amount`() {
        val discount = VoucherService.discountFor(voucher(value = "20", maxDiscount = "500000"), BigDecimal("10000000"))

        assertThat(discount).isEqualByComparingTo("500000")
    }

    @Test
    fun `fixed discount never exceeds the subtotal`() {
        val discount = VoucherService.discountFor(voucher(type = VoucherType.FIXED, value = "750000"), BigDecimal("500000"))

        assertThat(discount).isEqualByComparingTo("500000")
    }

    @Test
    fun `percentage discount is rounded to whole rupiah`() {
        val discount = VoucherService.discountFor(voucher(value = "15"), BigDecimal("333333"))

        // -- 49,999.95 rounds to 50,000 --
        assertThat(discount).isEqualByComparingTo("50000")
    }
    // -- end of region discount --

    // -- region of redeem / release --
    @Test
    fun `redeem counts one use and returns the discount`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(voucher(maxUsage = 10, used = 3))
        whenever(mockRepository.incrementUsageIfAvailable(1L)).thenReturn(1)

        val applied = service.redeem("welcome10", BigDecimal("1000000"))

        assertThat(applied.code).isEqualTo("WELCOME10")
        assertThat(applied.discountAmount).isEqualByComparingTo("100000")
        verify(mockRepository).incrementUsageIfAvailable(1L)
    }

    @Test
    fun `redeem fails when another order took the last use`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(voucher(maxUsage = 10, used = 9))
        whenever(mockRepository.incrementUsageIfAvailable(1L)).thenReturn(0)

        val exception = assertThrows<IllegalArgumentException> { service.redeem("WELCOME10", BigDecimal("1000000")) }

        assertThat(exception.message).contains("fully used")
    }

    @Test
    fun `redeem an invalid voucher fails without counting a use`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(voucher(active = false))

        assertThrows<IllegalArgumentException> { service.redeem("WELCOME10", BigDecimal("1000000")) }
        verify(mockRepository, never()).incrementUsageIfAvailable(any())
    }

    @Test
    fun `release gives back one use`() {
        whenever(mockRepository.findByCodeIgnoreCase("WELCOME10")).thenReturn(voucher(used = 2))

        service.release("WELCOME10")

        verify(mockRepository).decrementUsage(1L)
    }
    // -- end of region redeem / release --

    // -- region of backoffice --
    @Test
    fun `create stores the code upper case`() {
        whenever(mockRepository.save(any<Voucher>())).thenAnswer { (it.arguments[0] as Voucher).apply { id = 5L } }

        val result = service.create(request(code = " summer-25 "))

        assertThat(result.code).isEqualTo("SUMMER-25")
        assertThat(result.currentUsage).isZero()
    }

    @Test
    fun `create with a code that exists in another case is rejected`() {
        whenever(mockRepository.existsByCodeIgnoreCase("SUMMER-25")).thenReturn(true)

        assertThrows<IllegalArgumentException> { service.create(request()) }
        verify(mockRepository, never()).save(any<Voucher>())
    }

    @Test
    fun `create validates the fields`() {
        assertThrows<IllegalArgumentException> { service.create(request(code = "a b")) }
        assertThrows<IllegalArgumentException> { service.create(request(value = "120")) }
        assertThrows<IllegalArgumentException> { service.create(request(value = "0", type = VoucherType.FIXED)) }
        assertThrows<IllegalArgumentException> { service.create(request(maxUsage = 0)) }
        assertThrows<IllegalArgumentException> { service.create(request(start = now, end = now.minusDays(1))) }
    }

    @Test
    fun `update cannot lower max usage below the uses so far`() {
        whenever(mockRepository.findById(1L)).thenReturn(Optional.of(voucher(maxUsage = 50, used = 20)))

        val exception = assertThrows<IllegalArgumentException> {
            service.update(1L, request(code = "WELCOME10", value = "10", maxUsage = 10))
        }

        assertThat(exception.message).contains("20")
    }

    @Test
    fun `toggle switches the voucher off and on`() {
        val existing = voucher()
        whenever(mockRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(mockRepository.save(any<Voucher>())).thenAnswer { it.arguments[0] }

        assertThat(service.toggle(1L).isActive).isFalse()
        assertThat(service.toggle(1L).isActive).isTrue()
    }
    // -- end of region backoffice --
}
