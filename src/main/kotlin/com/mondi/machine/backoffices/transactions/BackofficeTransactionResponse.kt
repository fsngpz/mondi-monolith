package com.mondi.machine.backoffices.transactions

import java.math.BigDecimal

/**
 * The model class for response backoffice transaction.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-15
 */
data class BackofficeTransactionResponse(
  val id: Long,
  val productName: String,
  val price: BigDecimal,
  val certificateUrl: String
)