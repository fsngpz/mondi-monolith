package com.mondi.machine.backoffices.transactions

import java.math.BigDecimal
import java.time.OffsetDateTime
import org.springframework.web.multipart.MultipartFile

/**
 * The model class for backoffice transaction request.
 *
 * @author Ferdinand Sangap
 * @since 2024-05-08
 */
data class BackofficeTransactionRequest(
  val userId: Long,
  val productName: String,
  val price: BigDecimal,
  val certificateFile: MultipartFile,
  val purchasedAt: OffsetDateTime
)