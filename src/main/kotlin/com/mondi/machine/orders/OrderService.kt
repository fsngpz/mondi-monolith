package com.mondi.machine.orders

import com.mondi.machine.accounts.addresses.AddressService
import com.mondi.machine.accounts.profiles.ProfileService
import com.mondi.machine.exceptions.InsufficientStockException
import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductRepository
import com.mondi.machine.products.ProductStatus
import com.mondi.machine.storage.extensionOrDefault
import com.mondi.machine.storage.requireDocument
import com.mondi.machine.storage.supabase.SupabaseService
import com.mondi.machine.storage.supabase.SupabaseService.Companion.BUCKET_PAYMENT_PROOFS
import com.mondi.machine.vouchers.VoucherService
import java.math.BigDecimal
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile

/**
 * The service class for customer [Order] operations.
 *
 * Public methods return [OrderResponse] and map inside the transaction, because the order items
 * and product media are loaded lazily.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@Service
class OrderService(
    private val repository: OrderRepository,
    private val productRepository: ProductRepository,
    private val addressService: AddressService,
    private val profileService: ProfileService,
    private val supabaseService: SupabaseService,
    private val voucherService: VoucherService,
    transactionManager: PlatformTransactionManager
) {

    private val log = LoggerFactory.getLogger(OrderService::class.java)

    // -- used by the suspend upload method, which @Transactional does not cover --
    private val transactionTemplate = TransactionTemplate(transactionManager)

    /**
     * a function to get an [Order] of the user.
     *
     * @param orderId the order unique identifier.
     * @param userId the user unique identifier.
     * @return the [OrderResponse] instance.
     */
    @Transactional(readOnly = true)
    fun get(orderId: Long, userId: Long): OrderResponse {
        return findOwned(orderId, userId).toResponse()
    }

    /**
     * a function to find all [Order] of the user.
     *
     * @param userId the user unique identifier.
     * @param status the status to filter by, or null for all.
     * @param pageable the [Pageable].
     * @return the [Page] of [OrderResponse].
     */
    @Transactional(readOnly = true)
    fun findAll(userId: Long, status: OrderStatus?, pageable: Pageable): Page<OrderResponse> {
        return repository.findAllByProfileId(userId, status, pageable).map { it.toResponse() }
    }

    /**
     * a function to place a new [Order] from the cart.
     *
     * Validates every product (exists, active, enough stock), copies the product price and the
     * shipping address into the order, and takes the ordered quantity off the product stock.
     * The product rows stay locked until the transaction ends.
     *
     * @param userId the user unique identifier.
     * @param request the [CreateOrderRequest] instance.
     * @return the created [OrderResponse].
     */
    @Transactional
    fun create(userId: Long, request: CreateOrderRequest): OrderResponse {
        // -- validate the request --
        require(request.items.isNotEmpty()) {
            "field 'items' cannot be empty"
        }
        request.items.forEach {
            require(it.quantity > 0) {
                "quantity for product '${it.productId}' must be greater than 0"
            }
        }
        val notes = request.notes?.trim()?.ifEmpty { null }
        require(notes == null || notes.length <= MAX_NOTES_LENGTH) {
            "field 'notes' cannot be longer than $MAX_NOTES_LENGTH characters"
        }
        // -- merge repeated products into one line, keeping the cart order --
        val quantities = request.items
            .groupingBy { it.productId }
            .fold(0) { total, item -> total + item.quantity }

        // -- get the profile and the address, the address must belong to the user --
        val profile = profileService.get(userId)
        val address = addressService.getById(request.addressId, userId)

        // -- lock and validate the products --
        val productsById = productRepository.findAllByIdInForUpdate(quantities.keys).associateBy { it.id }
        val lines: List<Pair<Product, Int>> = quantities.map { (productId, quantity) ->
            val product = productsById[productId]
                ?: throw NoSuchElementException("no product data was found with id '$productId'")
            require(product.status == ProductStatus.ACTIVE) {
                "product '${product.name}' is no longer available"
            }
            if (product.stock < quantity) {
                throw InsufficientStockException(
                    "only ${product.stock} left in stock for product '${product.name}'"
                )
            }
            product to quantity
        }

        // -- calculate the amounts --
        val subtotal = lines.fold(BigDecimal.ZERO) { sum, (product, quantity) ->
            sum + product.sellingPrice() * quantity.toBigDecimal()
        }
        // -- use the voucher; this counts one use and is undone if the order fails --
        val applied = request.voucherCode?.trim()?.ifEmpty { null }?.let { voucherService.redeem(it, subtotal) }
        val discountAmount = applied?.discountAmount ?: BigDecimal.ZERO

        // -- setup the instance of Order --
        val order = Order(
            profile = profile,
            paymentMethod = request.paymentMethod,
            shippingRecipientName = address.recipientName,
            shippingPhone = address.phone,
            shippingAddressLine1 = address.addressLine1,
            shippingAddressLine2 = address.addressLine2,
            shippingCity = address.city,
            shippingState = address.state,
            shippingPostalCode = address.postalCode,
            shippingCountry = address.country,
            subtotal = subtotal,
            discountAmount = discountAmount,
            totalPrice = (subtotal - discountAmount).max(BigDecimal.ZERO)
        ).apply {
            this.voucherCode = applied?.code
            this.notes = notes
        }
        // -- add the items and take them off the stock --
        lines.forEach { (product, quantity) ->
            order.items.add(
                OrderItem(
                    order = order,
                    product = product,
                    productName = product.name,
                    productSku = product.sku,
                    price = product.sellingPrice(),
                    quantity = quantity
                )
            )
            product.stock -= quantity
        }
        productRepository.saveAll(lines.map { it.first })
        // -- save the order, the items are saved by cascade --
        val savedOrder = repository.save(order)
        log.info("Created order ${savedOrder.id} for user $userId with ${lines.size} item(s)")
        return savedOrder.toResponse()
    }

    /**
     * a function to upload the payment proof of a bank transfer.
     * Only allowed while the order is waiting for payment.
     *
     * @param orderId the order unique identifier.
     * @param userId the user unique identifier.
     * @param file the [MultipartFile] of the payment proof (image or PDF).
     * @return the updated [OrderResponse].
     */
    suspend fun uploadPaymentProof(orderId: Long, userId: Long, file: MultipartFile): OrderResponse {
        // -- validate before uploading, so a rejected request leaves no file behind --
        file.requireDocument()
        transactionTemplate.execute { requirePendingPayment(findOwned(orderId, userId)) }
        // -- upload the file --
        val fileName = "$userId/order-$orderId-${OffsetDateTime.now().toEpochSecond()}.${file.extensionOrDefault()}"
        val fileKey = supabaseService.uploadFile(BUCKET_PAYMENT_PROOFS, fileName, file)
            ?: throw IllegalArgumentException("the file path on upload payment proof is null")
        // -- store the key, re-checking the status in case it changed during the upload --
        return requireNotNull(transactionTemplate.execute {
            val order = findOwned(orderId, userId)
            requirePendingPayment(order)
            order.paymentProofUrl = fileKey
            repository.save(order).toResponse()
        })
    }

    /**
     * a private function to find an [Order] that belongs to the user.
     *
     * @param orderId the order unique identifier.
     * @param userId the user unique identifier.
     * @return the [Order] instance.
     */
    private fun findOwned(orderId: Long, userId: Long): Order {
        // -- an order of another user is reported as not found --
        return repository.findByIdAndProfileId(orderId, userId)
            ?: throw NoSuchElementException("no order was found with id '$orderId'")
    }

    /**
     * a private function to validate the order is still waiting for payment.
     *
     * @param order the [Order] instance.
     */
    private fun requirePendingPayment(order: Order) {
        require(order.status == OrderStatus.PENDING_PAYMENT) {
            "payment proof can only be uploaded while the order is waiting for payment"
        }
    }

    companion object {
        const val MAX_NOTES_LENGTH = 1000
    }
}
