package com.mondi.machine.backoffices.orders

import com.mondi.machine.exceptions.InvalidOrderStatusTransitionException
import com.mondi.machine.orders.Order
import com.mondi.machine.orders.OrderRepository
import com.mondi.machine.orders.OrderResponse
import com.mondi.machine.orders.OrderStatus
import com.mondi.machine.orders.ShippingCourier
import com.mondi.machine.orders.toResponse
import com.mondi.machine.products.ProductRepository
import com.mondi.machine.storage.extensionOrDefault
import com.mondi.machine.storage.requireDocument
import com.mondi.machine.storage.supabase.SupabaseService
import com.mondi.machine.storage.supabase.SupabaseService.Companion.BUCKET_CERTIFICATES
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile

/**
 * The service class for backoffice [Order] management.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@Service
class BackofficeOrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val supabaseService: SupabaseService,
    transactionManager: PlatformTransactionManager
) {

    private val log = LoggerFactory.getLogger(BackofficeOrderService::class.java)

    // -- used by the suspend upload method, which @Transactional does not cover --
    private val transactionTemplate = TransactionTemplate(transactionManager)

    /**
     * a function to find all orders.
     *
     * @param search the search term (order id, recipient, phone, customer email or name).
     * @param status the status to filter by, or null for all.
     * @param pageable the [Pageable].
     * @return the [Page] of [OrderResponse].
     */
    @Transactional(readOnly = true)
    fun findAll(search: String?, status: OrderStatus?, pageable: Pageable): Page<OrderResponse> {
        val term = search?.trim()?.ifEmpty { null }
        return orderRepository.findAllCustom(term, status, pageable).map { it.toResponse() }
    }

    /**
     * a function to get an order.
     *
     * @param orderId the order unique identifier.
     * @return the [OrderResponse] instance.
     */
    @Transactional(readOnly = true)
    fun get(orderId: Long): OrderResponse {
        return find(orderId).toResponse()
    }

    /**
     * a function to change the status of an order.
     * Only the transitions in [OrderStatus.allowedTransitions] are accepted.
     * Cancelling an order puts its items back in stock. Moving to [OrderStatus.SHIPPED] needs a
     * shipment: either already set with [updateShipment] or given here as [courier] and
     * [trackingNumber].
     *
     * @param orderId the order unique identifier.
     * @param status the new status.
     * @param courier the courier, to set the shipment in the same change (optional).
     * @param trackingNumber the courier tracking number (optional, required unless in-house).
     * @return the updated [OrderResponse].
     */
    @Transactional
    fun updateStatus(
        orderId: Long,
        status: OrderStatus,
        courier: ShippingCourier? = null,
        trackingNumber: String? = null
    ): OrderResponse {
        val order = find(orderId)
        val current = order.status
        // -- validate the transition --
        if (!current.canTransitionTo(status)) {
            throw InvalidOrderStatusTransitionException(
                "cannot change order status from $current to $status"
            )
        }
        // -- apply the shipment given with the status change --
        if (courier != null) {
            applyShipment(order, courier, trackingNumber)
        }
        if (status == OrderStatus.SHIPPED) {
            requireNotNull(order.shippingCourier) {
                "set the courier and tracking number before marking the order as shipped"
            }
            order.shippedAt = OffsetDateTime.now()
        }
        // -- put the items back in stock when the order is cancelled --
        if (status == OrderStatus.CANCELLED) {
            restock(order)
        }
        order.status = status
        val savedOrder = orderRepository.save(order)
        log.info("Changed order $orderId status from $current to $status")
        return savedOrder.toResponse()
    }

    /**
     * a function to set or correct the shipment (courier and tracking number) of an order.
     * Allowed once the payment is confirmed and until the order is delivered.
     *
     * @param orderId the order unique identifier.
     * @param courier the courier.
     * @param trackingNumber the courier tracking number, required unless [ShippingCourier.IN_HOUSE].
     * @return the updated [OrderResponse].
     */
    @Transactional
    fun updateShipment(orderId: Long, courier: ShippingCourier, trackingNumber: String?): OrderResponse {
        val order = find(orderId)
        require(order.status in SHIPMENT_EDITABLE_STATUSES) {
            "the shipment can only be set after the payment is confirmed, not while the order is ${order.status}"
        }
        applyShipment(order, courier, trackingNumber)
        val savedOrder = orderRepository.save(order)
        log.info("Set shipment of order $orderId to $courier")
        return savedOrder.toResponse()
    }

    /**
     * a function to upload the certificate of an order item.
     * Uploading again replaces the stored certificate.
     *
     * @param orderId the order unique identifier.
     * @param itemId the order item unique identifier.
     * @param file the [MultipartFile] of the certificate (image or PDF).
     * @return the updated [OrderResponse].
     */
    suspend fun uploadCertificate(orderId: Long, itemId: Long, file: MultipartFile): OrderResponse {
        // -- validate before uploading, so a rejected request leaves no file behind --
        file.requireDocument()
        val profileId = requireNotNull(transactionTemplate.execute {
            val order = find(orderId)
            requireCertificateAllowed(order, itemId)
            order.profile?.id ?: 0L
        })
        // -- upload the file --
        val fileName =
            "$profileId/order-$orderId-item-$itemId-${OffsetDateTime.now().toEpochSecond()}.${file.extensionOrDefault()}"
        val fileKey = supabaseService.uploadFile(BUCKET_CERTIFICATES, fileName, file)
            ?: throw IllegalArgumentException("the file path on upload certificate is null")
        // -- store the key on the item --
        return requireNotNull(transactionTemplate.execute {
            val order = find(orderId)
            requireCertificateAllowed(order, itemId)
            order.items.first { it.id == itemId }.certificateUrl = fileKey
            orderRepository.save(order).toResponse()
        })
    }

    /**
     * a private function to find an order.
     *
     * @param orderId the order unique identifier.
     * @return the [Order] instance.
     */
    private fun find(orderId: Long): Order {
        return orderRepository.findByIdOrNull(orderId)
            ?: throw NoSuchElementException("no order was found with id '$orderId'")
    }

    /**
     * a private function to validate a certificate can be attached to the order item.
     *
     * @param order the [Order] instance.
     * @param itemId the order item unique identifier.
     */
    private fun requireCertificateAllowed(order: Order, itemId: Long) {
        if (order.items.none { it.id == itemId }) {
            throw NoSuchElementException("no item with id '$itemId' was found in order '${order.id}'")
        }
        require(order.status != OrderStatus.CANCELLED) {
            "cannot upload a certificate for a cancelled order"
        }
    }

    /**
     * a private function to validate and set the shipment fields of an order.
     *
     * @param order the [Order] instance.
     * @param courier the courier.
     * @param trackingNumber the tracking number, or null.
     */
    private fun applyShipment(order: Order, courier: ShippingCourier, trackingNumber: String?) {
        val number = trackingNumber?.trim()?.ifEmpty { null }
        if (courier.requiresTrackingNumber) {
            requireNotNull(number) {
                "a tracking number is required for $courier"
            }
        }
        require(number == null || TRACKING_NUMBER_PATTERN.matches(number)) {
            "the tracking number must be 4 to 64 letters, digits or dashes"
        }
        order.shippingCourier = courier
        order.trackingNumber = number
    }

    /**
     * a private function to put the items of an order back in stock.
     * Items whose product was deleted are skipped.
     *
     * @param order the [Order] instance.
     */
    private fun restock(order: Order) {
        val products = order.items.mapNotNull { item ->
            item.product?.also { it.stock += item.quantity }
        }
        if (products.isNotEmpty()) {
            productRepository.saveAll(products)
        }
    }

    companion object {
        // -- the shipment can be set once payment is confirmed, and corrected until delivered --
        val SHIPMENT_EDITABLE_STATUSES = setOf(
            OrderStatus.PAYMENT_CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
        )
        private val TRACKING_NUMBER_PATTERN = Regex("^[A-Za-z0-9-]{4,64}$")
    }
}
