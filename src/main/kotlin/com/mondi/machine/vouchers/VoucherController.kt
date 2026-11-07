package com.mondi.machine.vouchers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for checking voucher codes at checkout.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Tag(name = "Voucher API")
@RestController
@RequestMapping("/v1/vouchers")
class VoucherController(private val service: VoucherService) {

    /**
     * a POST request to check a voucher code for an order subtotal, without using it.
     */
    @Operation(
        summary = "Check a voucher",
        description = "Returns valid, the discount for the subtotal and a message. The voucher is only used when the order is placed."
    )
    @PostMapping("/validate")
    fun validate(@RequestBody request: VoucherValidationRequest): VoucherValidationResponse {
        // -- validate the voucher --
        return service.validate(request.code, request.orderSubtotal)
    }
}
