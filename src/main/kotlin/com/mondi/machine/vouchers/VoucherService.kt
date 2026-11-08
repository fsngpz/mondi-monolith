package com.mondi.machine.vouchers

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The service class for discount [Voucher] codes.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Service
class VoucherService(private val repository: VoucherRepository) {

    private val log = LoggerFactory.getLogger(VoucherService::class.java)

    /**
     * a function to check a voucher code for a subtotal without using it.
     * An invalid code is not an error; the response says why.
     *
     * @param code the voucher code.
     * @param orderSubtotal the order subtotal.
     * @return the [VoucherValidationResponse].
     */
    @Transactional(readOnly = true)
    fun validate(code: String, orderSubtotal: BigDecimal): VoucherValidationResponse {
        val normalized = normalizeCode(code)
        val voucher = repository.findByCodeIgnoreCase(normalized)
        val problem = problemWith(voucher, orderSubtotal)
        return if (voucher == null || problem != null) {
            VoucherValidationResponse(false, normalized, BigDecimal.ZERO, problem ?: NOT_FOUND)
        } else {
            val discount = discountFor(voucher, orderSubtotal)
            VoucherValidationResponse(true, voucher.code, discount, "Voucher applied")
        }
    }

    /**
     * a function to use a voucher on an order: validates it, counts one use and returns the discount.
     * Must run inside the order's transaction so the use is undone if the order fails.
     *
     * @param code the voucher code.
     * @param orderSubtotal the order subtotal.
     * @return the [AppliedVoucher] with the stored code and discount.
     */
    @Transactional
    fun redeem(code: String, orderSubtotal: BigDecimal): AppliedVoucher {
        val voucher = repository.findByCodeIgnoreCase(normalizeCode(code))
        val problem = problemWith(voucher, orderSubtotal)
        require(voucher != null && problem == null) { problem ?: NOT_FOUND }
        // -- count the use atomically; another order may have taken the last one --
        require(repository.incrementUsageIfAvailable(requireNotNull(voucher.id)) == 1) {
            "this voucher has been fully used"
        }
        log.info("Redeemed voucher ${voucher.code}")
        return AppliedVoucher(voucher.code, discountFor(voucher, orderSubtotal))
    }

    /**
     * a function to give back the use of a voucher, e.g. when its order is cancelled.
     * Unknown codes are ignored (the voucher may have been deleted).
     *
     * @param code the voucher code stored on the order.
     */
    @Transactional
    fun release(code: String) {
        val voucher = repository.findByCodeIgnoreCase(code) ?: return
        repository.decrementUsage(requireNotNull(voucher.id))
        log.info("Released one use of voucher ${voucher.code}")
    }

    // -- region of backoffice --

    /**
     * a function to find all vouchers.
     */
    @Transactional(readOnly = true)
    fun findAll(search: String?, isActive: Boolean?, pageable: Pageable): Page<VoucherResponse> {
        return repository.findAllCustom(search?.trim()?.ifEmpty { null }, isActive, pageable).map { it.toResponse() }
    }

    /**
     * a function to get a voucher.
     */
    @Transactional(readOnly = true)
    fun get(id: Long): VoucherResponse = find(id).toResponse()

    /**
     * a function to create a voucher.
     *
     * @param request the [VoucherRequest] instance.
     * @return the created [VoucherResponse].
     */
    @Transactional
    fun create(request: VoucherRequest): VoucherResponse {
        val code = validateRequest(request)
        require(!repository.existsByCodeIgnoreCase(code)) {
            "voucher code '$code' already exists"
        }
        val voucher = Voucher(
            code = code,
            description = request.description?.trim()?.ifEmpty { null },
            type = request.type,
            value = request.value,
            maxDiscountAmount = request.maxDiscountAmount,
            minOrderAmount = request.minOrderAmount ?: BigDecimal.ZERO,
            maxUsage = request.maxUsage,
            startDate = request.startDate,
            endDate = request.endDate,
            isActive = request.isActive
        )
        return repository.save(voucher).toResponse().also { log.info("Created voucher $code") }
    }

    /**
     * a function to update a voucher. The usage count is kept.
     *
     * @param id the voucher unique identifier.
     * @param request the [VoucherRequest] instance.
     * @return the updated [VoucherResponse].
     */
    @Transactional
    fun update(id: Long, request: VoucherRequest): VoucherResponse {
        val voucher = find(id)
        val code = validateRequest(request)
        if (!code.equals(voucher.code, ignoreCase = true)) {
            require(!repository.existsByCodeIgnoreCase(code)) { "voucher code '$code' already exists" }
        }
        require(request.maxUsage == null || request.maxUsage >= voucher.currentUsage) {
            "max usage cannot be lower than the ${voucher.currentUsage} time(s) it was already used"
        }
        voucher.apply {
            this.code = code
            this.description = request.description?.trim()?.ifEmpty { null }
            this.type = request.type
            this.value = request.value
            this.maxDiscountAmount = request.maxDiscountAmount
            this.minOrderAmount = request.minOrderAmount ?: BigDecimal.ZERO
            this.maxUsage = request.maxUsage
            this.startDate = request.startDate
            this.endDate = request.endDate
            this.isActive = request.isActive
        }
        return repository.save(voucher).toResponse()
    }

    /**
     * a function to switch a voucher on or off.
     */
    @Transactional
    fun toggle(id: Long): VoucherResponse {
        val voucher = find(id)
        voucher.isActive = !voucher.isActive
        return repository.save(voucher).toResponse()
    }

    // -- end of region backoffice --

    private fun find(id: Long): Voucher =
        repository.findByIdOrNull(id) ?: throw NoSuchElementException("no voucher was found with id '$id'")

    /**
     * a private function to explain why the voucher cannot be used, or null when it can.
     */
    private fun problemWith(voucher: Voucher?, orderSubtotal: BigDecimal): String? {
        val now = OffsetDateTime.now()
        return when {
            voucher == null -> NOT_FOUND
            !voucher.isActive -> "this voucher is not active"
            now.isBefore(voucher.startDate) -> "this voucher is not valid yet"
            now.isAfter(voucher.endDate) -> "this voucher has expired"
            voucher.maxUsage != null && voucher.currentUsage >= voucher.maxUsage!! -> "this voucher has been fully used"
            orderSubtotal < voucher.minOrderAmount ->
                "this voucher needs a minimum order of IDR ${voucher.minOrderAmount.setScale(0, RoundingMode.DOWN).toPlainString()}"
            else -> null
        }
    }

    /**
     * a private function to validate a create / update request.
     *
     * @return the normalized code.
     */
    private fun validateRequest(request: VoucherRequest): String {
        val code = normalizeCode(request.code)
        require(CODE_PATTERN.matches(code)) {
            "the code must be 3 to 32 letters, digits, dashes or underscores"
        }
        require(request.value.signum() > 0) { "field 'value' must be greater than 0" }
        if (request.type == VoucherType.PERCENTAGE) {
            require(request.value <= BigDecimal(100)) { "a percentage voucher cannot be more than 100%" }
        }
        require(request.maxDiscountAmount == null || request.maxDiscountAmount.signum() > 0) {
            "field 'maxDiscountAmount' must be greater than 0"
        }
        require(request.minOrderAmount == null || request.minOrderAmount.signum() >= 0) {
            "field 'minOrderAmount' cannot be negative"
        }
        require(request.maxUsage == null || request.maxUsage >= 1) { "field 'maxUsage' must be at least 1" }
        require(request.startDate.isBefore(request.endDate)) { "the start date must be before the end date" }
        return code
    }

    companion object {
        private const val NOT_FOUND = "this voucher code does not exist"
        private val CODE_PATTERN = Regex("^[A-Z0-9_-]{3,32}$")

        fun normalizeCode(code: String): String = code.trim().uppercase()

        /**
         * the discount of a voucher on a subtotal, in whole rupiah, never more than the subtotal.
         *
         * @param voucher the [Voucher].
         * @param orderSubtotal the order subtotal.
         * @return the discount amount.
         */
        fun discountFor(voucher: Voucher, orderSubtotal: BigDecimal): BigDecimal {
            val raw = when (voucher.type) {
                VoucherType.PERCENTAGE -> {
                    val percent = orderSubtotal * voucher.value / BigDecimal(100)
                    voucher.maxDiscountAmount?.let { percent.min(it) } ?: percent
                }
                VoucherType.FIXED -> voucher.value
            }
            return raw.min(orderSubtotal).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP)
        }
    }
}

/**
 * The voucher applied to an order: the stored code and the discount amount.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
data class AppliedVoucher(val code: String, val discountAmount: BigDecimal)
