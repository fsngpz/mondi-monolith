package com.mondi.machine.backoffices.orders

import com.mondi.machine.exceptions.ErrorResponse
import com.mondi.machine.orders.OrderResponse
import com.mondi.machine.orders.OrderStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.multipart.MultipartFile

/**
 * The Swagger documentation interface for backoffice order management.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@Tag(name = "Backoffice Order API")
interface BackofficeOrderSwaggerController {

    @Operation(
        summary = "List orders",
        description = "Paginated list of all orders. `search` matches the order id, recipient name, phone, customer email or name."
    )
    fun findAll(search: String?, status: OrderStatus?, pageable: Pageable): Page<OrderResponse>

    @Operation(summary = "Get order", description = "Get an order with its items and customer")
    fun get(orderId: Long): OrderResponse

    @Operation(
        summary = "Update order status",
        description = "Allowed: PENDING_PAYMENT → PAYMENT_CONFIRMED/CANCELLED, PAYMENT_CONFIRMED → PROCESSING/CANCELLED, " +
            "PROCESSING → SHIPPED/CANCELLED, SHIPPED → DELIVERED. Cancelling puts the items back in stock. " +
            "SHIPPED needs a courier and tracking number, set before or sent with this request."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Status updated",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Transition not allowed",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun updateStatus(orderId: Long, request: BackofficeOrderStatusRequest): OrderResponse

    @Operation(
        summary = "Set order shipment",
        description = "Set or correct the courier and tracking number. Allowed from PAYMENT_CONFIRMED until DELIVERED. " +
            "The tracking number is required unless the courier is IN_HOUSE."
    )
    fun updateShipment(orderId: Long, request: BackofficeOrderShipmentRequest): OrderResponse

    @Operation(
        summary = "Upload item certificate",
        description = "Upload the certificate (image or PDF) for an order item. Replaces any existing certificate."
    )
    suspend fun uploadCertificate(orderId: Long, itemId: Long, file: MultipartFile): OrderResponse
}
