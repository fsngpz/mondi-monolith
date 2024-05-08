package com.mondi.machine.backoffices.transactions

import java.math.BigDecimal
import java.time.OffsetDateTime
import org.springframework.web.multipart.MultipartFile

/**
 * The model class for backoffice transaction nullable request.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-15
 */
data class BackofficeTransactionNullableRequest(
  val userId: Long? = null,
  val productName: String? = null,
  val price: BigDecimal? = null,
  val certificateFile: MultipartFile? = null,
  val purchasedAt: OffsetDateTime? = null
)