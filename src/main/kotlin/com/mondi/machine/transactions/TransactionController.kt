package com.mondi.machine.transactions

import jakarta.servlet.http.HttpServletRequest
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for [Transaction] features.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
@RestController
@RequestMapping("/v1/transactions")
class TransactionController(private val service: TransactionService) : TransactionSwaggerController {

  /**
   * a GET request to find all [Transaction] that have been done by specific user.
   *
   * @param search the parameter to filter data by product name.
   * @param purchasedAtFrom the start date to filter data by purchased at.
   * @param purchasedAtTo the end date to filter data by purchased at.
   * @param httpServletRequest the [HttpServletRequest].
   * @return the [Page] of [TransactionResponse].
   */
  @GetMapping
  override fun findAll(
    search: String?,
    purchasedAtFrom: OffsetDateTime?,
    purchasedAtTo: OffsetDateTime?,
    @PageableDefault(sort = ["purchasedAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    httpServletRequest: HttpServletRequest
  ): Page<TransactionResponse> {
    // -- get the ID from header --
    val id = httpServletRequest.getHeader("ID").toLong()
    // -- find all data --
    return service.findAll(
      id,
      search,
      purchasedAtFrom ?: OffsetDateTime.of(LocalDate.EPOCH, LocalTime.MIN, ZoneOffset.UTC),
      purchasedAtTo ?: OffsetDateTime.now(),
      pageable
    )
  }
}