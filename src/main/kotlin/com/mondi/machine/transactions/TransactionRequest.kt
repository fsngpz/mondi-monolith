package com.mondi.machine.transactions

import java.math.BigDecimal
import java.time.OffsetDateTime
import org.springframework.web.multipart.MultipartFile

/**
 * The model class of request [Transaction].
 *
 * @author Ferdinand Sangap
 * @since 2024-05-08
 */
data class TransactionRequest(
  val productName: String,
  val price: BigDecimal,
  val certificateFile: MultipartFile,
  val purchasedAt: OffsetDateTime
)