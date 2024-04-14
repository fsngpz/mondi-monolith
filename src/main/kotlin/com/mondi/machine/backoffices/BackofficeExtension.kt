package com.mondi.machine.backoffices

import com.mondi.machine.auths.users.UserRole
import com.mondi.machine.backoffices.accounts.BackofficeAccountResponse

/**
 * an extension function to map the [UserRole] instance to [BackofficeAccountResponse].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
fun UserRole.toResponse(): BackofficeAccountResponse {
  val userId = this.user.id
  // -- map the roles to List of String name role --
  val userRoles = this.user.roles.map { it.role.name }
  // -- validate the field userId --
  requireNotNull(userId) {
    "the user id is null"
  }// -- return the mapped value --
  return BackofficeAccountResponse(
    userId,
    this.user.email,
    this.user.username,
    userRoles
  )
}