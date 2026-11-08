package com.mondi.machine.backoffices.vouchers

import com.mondi.machine.vouchers.VoucherRequest
import com.mondi.machine.vouchers.VoucherResponse
import com.mondi.machine.vouchers.VoucherService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for backoffice voucher management.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Tag(name = "Backoffice Voucher API")
@RestController
@RequestMapping("/v1/backoffice/vouchers")
class BackofficeVoucherController(private val service: VoucherService) {

    @Operation(summary = "List vouchers", description = "Paginated. `search` matches code and description; `isActive` filters.")
    @GetMapping
    fun findAll(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) isActive: Boolean?,
        @PageableDefault(sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<VoucherResponse> = service.findAll(search, isActive, pageable)

    @Operation(summary = "Get voucher")
    @GetMapping("/{voucherId}")
    fun get(@PathVariable voucherId: Long): VoucherResponse = service.get(voucherId)

    @Operation(
        summary = "Create voucher",
        description = "Code: 3-32 letters, digits, - or _ (stored upper case). PERCENTAGE value 1-100, optional maxDiscountAmount; FIXED value in IDR."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: VoucherRequest): VoucherResponse = service.create(request)

    @Operation(summary = "Update voucher", description = "The usage count is kept; maxUsage cannot go below it.")
    @PutMapping("/{voucherId}")
    fun update(@PathVariable voucherId: Long, @RequestBody request: VoucherRequest): VoucherResponse =
        service.update(voucherId, request)

    @Operation(summary = "Switch voucher on or off")
    @PatchMapping("/{voucherId}/toggle")
    fun toggle(@PathVariable voucherId: Long): VoucherResponse = service.toggle(voucherId)
}
