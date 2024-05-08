package com.mondi.machine.transactions

import com.mondi.machine.accounts.profiles.ProfileService
import com.mondi.machine.storage.dropbox.DropboxService
import java.math.BigDecimal
import java.time.OffsetDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * The service class for [Transaction].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
@Service
class TransactionService(
  private val profileService: ProfileService,
  private val dropboxService: DropboxService,
  private val repository: TransactionRepository
) {

  /**
   * a function to get the [Transaction] instance.
   *
   * @param id the unique identifier of [Transaction].
   * @return the [Transaction] instance.
   */
  fun get(id: Long): Transaction {
    return repository.findByIdOrNull(id)
      ?: throw NoSuchElementException("no transaction data was found with id '$id'")
  }

  /**
   * a function to find all [Transaction] of specified user.
   *
   * @param id the user / profile unique identifier.
   * @param search the parameter to filter data by product name.
   * @param purchasedAtFrom the starts date to filter data by purchased at.
   * @param purchasedAtTo the ends date to filter data by purchased at.
   * @param pageable the [Pageable].
   * @return the [Page] of [TransactionResponse].
   */
  fun findAll(
    id: Long,
    search: String?,
    purchasedAtFrom: OffsetDateTime,
    purchasedAtTo: OffsetDateTime,
    pageable: Pageable
  ): Page<TransactionResponse> {
    // -- find the data --
    return repository.findAllCustom(id, search, purchasedAtFrom, purchasedAtTo, pageable)
      .map { it.toResponse() }
  }

  /**
   * a function to handle create new [Transaction].
   *
   * @param id the user / profile unique identifier.
   * @param productName the name of product.
   * @param price the price of transaction.
   * @param certificateFile the [MultipartFile] of product certificate.
   * @param purchasedAt the purchased data.
   * @return the [Transaction] instance.
   */
  fun create(
    id: Long,
    productName: String,
    price: BigDecimal,
    certificateFile: MultipartFile,
    purchasedAt: OffsetDateTime
  ): Transaction {
    // -- get the profile --
    val profile = profileService.get(id)
    // -- upload the certificate image --
    val certificateUrl = dropboxService.upload(id, certificateFile)
    // -- setup the instance of Transaction --
    val transaction = Transaction(productName, price, certificateUrl, purchasedAt, profile)
    // -- save the instance to database --
    return repository.save(transaction)
  }

  /**
   * a function to update the instance of [Transaction].
   *
   * @param id the [Transaction] unique identifier.
   * @param request the [TransactionRequest] instance.
   * @return the [Transaction] instance.
   */
  fun update(id: Long, request: TransactionRequest): Transaction {
    // -- get the transaction instance --
    val transaction = get(id)
    // -- validate the id of user --
    val userId = requireNotNull(transaction.profile.user.id) { "the id of user is null" }
    // -- upload the certificate image --
    val certificateUrl = dropboxService.upload(userId, request.certificateFile)
    // -- update the instance --
    transaction.apply {
      this.productName = request.productName
      this.price = request.price
      this.certificateUrl = certificateUrl
      this.purchasedAt = request.purchasedAt
    }
    // -- save the updated instance --
    return repository.save(transaction)
  }
}