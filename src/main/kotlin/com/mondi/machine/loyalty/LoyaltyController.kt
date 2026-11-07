package com.mondi.machine.loyalty

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for the signed-in customer's loyalty points.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Tag(name = "Loyalty API")
@RestController
@RequestMapping("/v1/account/loyalty")
class LoyaltyController(private val service: LoyaltyService) {

    @Operation(summary = "My loyalty status", description = "Points balance, tier and points to the next tier")
    @GetMapping
    fun getBalance(@Parameter(hidden = true) @RequestAttribute("ID") userId: Long): LoyaltyResponse =
        service.getBalance(userId)

    @Operation(summary = "My points history", description = "Paginated, newest first")
    @GetMapping("/history")
    fun getHistory(
        @Parameter(hidden = true) @RequestAttribute("ID") userId: Long,
        @PageableDefault(sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<LoyaltyHistoryResponse> = service.getHistory(userId, pageable)
}
