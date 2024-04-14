package com.mondi.machine.transactions

import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * The model class for response Transaction.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-15
 */
data class TransactionResponse(
  val productName: String,
  val price: BigDecimal,
  val certificateUrl: String,
  val purchasedAt: OffsetDateTime
)