package com.mondi.machine.storage

import com.mondi.machine.accounts.profiles.ProfileRepository
import com.mondi.machine.orders.OrderItemRepository
import com.mondi.machine.orders.OrderRepository
import com.mondi.machine.transactions.TransactionRepository
import org.springframework.stereotype.Service

/**
 * The service class that decides who may open a private storage file.
 *
 * A customer may open a file only when it is stored on one of their own records: their profile
 * picture, a payment proof of their order, a certificate of their order item, or a legacy
 * transaction certificate. Admins may open any file.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Service
class StorageAccessService(
    private val profileRepository: ProfileRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val transactionRepository: TransactionRepository
) {

    /**
     * a function to validate the user may open the file.
     * A file the user may not open is reported as not found, so keys of other users are not confirmed.
     *
     * @param userId the user unique identifier.
     * @param isAdmin whether the user is an admin.
     * @param fileKey the storage key.
     */
    fun requireAccess(userId: Long, isAdmin: Boolean, fileKey: String) {
        if (isAdmin || isOwner(userId, fileKey)) return
        throw NoSuchElementException("no file was found with key '$fileKey'")
    }

    /**
     * a private function to check whether the file is stored on one of the user's records.
     *
     * @param userId the user unique identifier.
     * @param fileKey the storage key.
     * @return true if the user owns it.
     */
    private fun isOwner(userId: Long, fileKey: String): Boolean {
        return profileRepository.existsByIdAndProfilePictureUrl(userId, fileKey) ||
            orderRepository.existsByProfileIdAndPaymentProofUrl(userId, fileKey) ||
            orderItemRepository.existsByOrder_Profile_IdAndCertificateUrl(userId, fileKey) ||
            transactionRepository.existsByProfileIdAndCertificateUrl(userId, fileKey)
    }
}
