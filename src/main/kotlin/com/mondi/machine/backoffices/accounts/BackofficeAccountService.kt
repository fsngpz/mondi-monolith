package com.mondi.machine.backoffices.accounts

import com.mondi.machine.auths.users.UserRoleService
import com.mondi.machine.backoffices.toResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * The service class for backoffice related to account.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
@Service
class BackofficeAccountService(private val userService: UserRoleService) {

  /**
   * a function to find all user data.
   *
   * @param search the parameter for filter data by email or username.
   * @param role the parameter to filter data by role.
   * @param pageable the [Pageable].
   * @return the [Page] of [BackofficeAccountResponse].
   */
  fun findAll(search: String?, role: String?, pageable: Pageable): Page<BackofficeAccountResponse> {
    // -- find all data and map the response --
    return userService.findAll(search, role, pageable).map { it.toResponse() }
  }
}