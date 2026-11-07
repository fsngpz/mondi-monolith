package com.mondi.machine.orders

import com.mondi.machine.transactions.TransactionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The service class for certificates attached to order items.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@Service
class CertificateService(
    private val orderItemRepository: OrderItemRepository,
    private val transactionRepository: TransactionRepository
) {

    /**
     * a function to find all certificates of a user: order items with a certificate, followed by
     * legacy transaction certificates.
     *
     * @param userId the user / profile unique identifier.
     * @return the list of [CertificateResponse].
     */
    @Transactional(readOnly = true)
    fun findAllByUser(userId: Long): List<CertificateResponse> {
        val fromOrders = orderItemRepository.findAllWithCertificateByProfileId(userId, OrderStatus.CANCELLED).map { item ->
            CertificateResponse(
                source = CertificateSource.ORDER,
                orderItemId = item.id,
                orderId = item.order.id,
                productName = item.productName,
                productSku = item.productSku,
                certificateUrl = requireNotNull(item.certificateUrl),
                mediaUrl = item.product?.media?.minByOrNull { it.displayOrder }?.mediaUrl,
                purchasedAt = item.order.createdAt
            )
        }
        val fromLegacy = transactionRepository
            .findAllByProfileId(userId, Sort.by(Sort.Direction.DESC, "purchasedAt"))
            .map { transaction ->
                CertificateResponse(
                    source = CertificateSource.LEGACY_TRANSACTION,
                    orderItemId = null,
                    orderId = null,
                    productName = transaction.productName,
                    productSku = null,
                    certificateUrl = transaction.certificateUrl,
                    mediaUrl = null,
                    purchasedAt = transaction.purchasedAt
                )
            }
        return fromOrders + fromLegacy
    }

    /**
     * a function to find all order item certificates for the backoffice.
     *
     * @param search the search term, or null.
     * @param pageable the [Pageable].
     * @return the [Page] of [BackofficeCertificateResponse].
     */
    @Transactional(readOnly = true)
    fun findAll(search: String?, pageable: Pageable): Page<BackofficeCertificateResponse> {
        val term = search?.trim()?.ifEmpty { null }
        return orderItemRepository.findAllWithCertificate(term, pageable).map { item ->
            val order = item.order
            BackofficeCertificateResponse(
                orderItemId = requireNotNull(item.id),
                orderId = requireNotNull(order.id),
                orderStatus = order.status,
                productName = item.productName,
                productSku = item.productSku,
                certificateUrl = requireNotNull(item.certificateUrl),
                customer = order.toResponse().customer,
                purchasedAt = order.createdAt,
                uploadedAt = item.updatedAt
            )
        }
    }
}
