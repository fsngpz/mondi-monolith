package com.mondi.machine.backoffices

import com.mondi.machine.auths.users.UserRole
import com.mondi.machine.backoffices.accounts.BackofficeAccountResponse
import com.mondi.machine.backoffices.products.BackofficeProductNullableRequest
import com.mondi.machine.backoffices.products.BackofficeProductRequest
import com.mondi.machine.backoffices.products.BackofficeProductResponse
import com.mondi.machine.backoffices.transactions.BackofficeTransactionNullableRequest
import com.mondi.machine.backoffices.transactions.BackofficeTransactionRequest
import com.mondi.machine.backoffices.transactions.BackofficeTransactionResponse
import com.mondi.machine.products.Product
import com.mondi.machine.transactions.Transaction

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

/**
 * an extension function to map the [Transaction] to [BackofficeAccountResponse].
 *
 * @return the [BackofficeTransactionResponse].
 */
fun Transaction.toResponse(): BackofficeTransactionResponse {
  val id = this.id
  // -- validate field id --
  requireNotNull(id) {
    "field id is null"
  }
  // -- return the mapped value --
  return BackofficeTransactionResponse(id, this.productName, this.price, this.certificateUrl)
}

/**
 * an extension function to convert the [BackofficeTransactionNullableRequest]
 * to [BackofficeTransactionRequest].
 *
 * @return the [BackofficeTransactionResponse] instance.
 */
fun BackofficeTransactionNullableRequest.toNotNull(): BackofficeTransactionRequest {
  // -- validate field userId --
  requireNotNull(this.userId) {
    "the field 'userId' cannot be null"
  }
  // -- validate field productName --
  requireNotNull(this.productName) {
    "the field 'productName' cannot be null"
  }
  // -- validate field price --
  requireNotNull(this.price) {
    "the field 'price' cannot be null"

  }
  // -- validate field certificateFile --
  requireNotNull(this.certificateFile) {
    "the field 'certificateFile' cannot be null"

  }
  // -- validate field purchasedAt --
  requireNotNull(this.purchasedAt) {
    "the field 'purchasedAt' cannot be null"

  }
  // -- return the instance of BackofficeTransactionRequest --
  return BackofficeTransactionRequest(
    this.userId,
    this.productName,
    this.price,
    this.certificateFile,
    this.purchasedAt
  )
}

/**
 * an extension function to map the [Product] to [BackofficeProductResponse].
 *
 * @return the [BackofficeProductResponse].
 */
fun Product.toResponse(): BackofficeProductResponse {
  val id = this.id
  // -- validate field id --
  requireNotNull(id) {
    "field id is null"
  }
  // -- return the mapped value --
  return BackofficeProductResponse(
    id,
    this.name,
    this.description,
    this.price,
    this.currency,
    this.specificationInHtml,
    this.discountPercentage,
    this.media.map { it.mediaUrl },
    this.category,
    this.stock
  )
}

/**
 * an extension function to convert the [BackofficeProductNullableRequest]
 * to [BackofficeProductRequest].
 *
 * @return the [BackofficeProductRequest] instance.
 */
fun BackofficeProductNullableRequest.toNotNull(): BackofficeProductRequest {
  // -- validate field name --
  requireNotNull(this.name) {
    "the field 'name' cannot be null"
  }
  // -- validate field price --
  requireNotNull(this.price) {
    "the field 'price' cannot be null"
  }
  // -- validate field currency --
  requireNotNull(this.currency) {
    "the field 'currency' cannot be null"
  }
  // -- validate field discountPercentage --
  requireNotNull(this.discountPercentage) {
    "the field 'discountPercentage' cannot be null"
  }
  // -- validate field mediaFiles --
  requireNotNull(this.mediaFiles) {
    "the field 'mediaFiles' cannot be null"
  }
  // -- validate field category --
  requireNotNull(this.category) {
    "the field 'category' cannot be null"
  }
  // -- validate field stock --
  requireNotNull(this.stock) {
    "the field 'stock' cannot be null"
  }
  // -- return the instance of BackofficeProductRequest --
  return BackofficeProductRequest(
    this.name,
    this.description,
    this.price,
    this.currency,
    this.specificationInHtml,
    this.discountPercentage,
    this.mediaFiles,
    this.category,
    this.stock
  )
}