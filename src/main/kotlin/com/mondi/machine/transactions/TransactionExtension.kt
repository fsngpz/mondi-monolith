package com.mondi.machine.transactions

/**
 * an extension function to map the [Transaction] to [TransactionResponse].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-15
 */
fun Transaction.toResponse(): TransactionResponse {
  return TransactionResponse(this.productName, this.price, this.certificateUrl, this.purchasedAt)
}