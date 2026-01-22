package com.mondi.machine.backoffices.accounts

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The controller class for backoffice related to account.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
@RestController
@RequestMapping("/v1/backoffice/accounts")
class BackofficeAccountController(private val service: BackofficeAccountService) : BackofficeAccountSwaggerController {

  /**
   * a controller to handle request find accounts data.
   *
   * @param search the parameter for filter data by email or username.
   * @param role the parameter to filter data by role.
   * @param pageable the [Pageable] defaults sort [user.createdAt]
   * with direction [Sort.Direction.DESC].
   * @return [Page] of [BackofficeAccountResponse].
   */
  @GetMapping
  override fun findAll(
    @RequestParam search: String?,
    @RequestParam role: String?,
    @PageableDefault(sort = ["user.createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
  ): Page<BackofficeAccountResponse> {
    // -- find the data --
    return service.findAll(search, role, pageable)
  }
}