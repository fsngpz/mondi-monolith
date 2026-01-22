package com.mondi.machine.backoffices.transactions

import io.swagger.v3.oas.annotations.media.Schema
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
  @Schema(description = "User ID", example = "1")
  val userId: Long? = null,
  @Schema(description = "Product name", example = "Gold Necklace")
  val productName: String? = null,
  @Schema(description = "Transaction price", example = "2500.00")
  val price: BigDecimal? = null,
  @Schema(description = "Certificate file", type = "string", format = "binary")
  val certificateFile: MultipartFile? = null,
  @Schema(description = "Purchased date", example = "2024-01-15T10:30:00Z")
  val purchasedAt: OffsetDateTime? = null
)