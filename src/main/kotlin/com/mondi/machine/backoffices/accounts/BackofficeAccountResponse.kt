package com.mondi.machine.backoffices.accounts

/**
 * The model class for backoffice account response.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
data class BackofficeAccountResponse(
  val id: Long,
  val email: String,
  val name: String?,
  val roles: List<String>
)