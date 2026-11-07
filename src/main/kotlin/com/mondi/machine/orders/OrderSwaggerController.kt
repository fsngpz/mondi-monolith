package com.mondi.machine.orders

import com.mondi.machine.exceptions.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.multipart.MultipartFile

/**
 * The Swagger documentation interface for customer [Order] operations.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@Tag(name = "Order API")
interface OrderSwaggerController {

    @Operation(
        summary = "Place an order",
        description = "Create an order from the cart. Validates stock, copies prices and the shipping address, and takes the items off stock."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Order created",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request, e.g. an inactive product or an address of another user",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Product or address not found",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Not enough stock",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun create(request: CreateOrderRequest, @Parameter(hidden = true) userId: Long): OrderResponse

    @Operation(summary = "List my orders", description = "Paginated list of the authenticated user's orders, newest first")
    fun findAll(status: OrderStatus?, pageable: Pageable, @Parameter(hidden = true) userId: Long): Page<OrderResponse>

    @Operation(summary = "Get my order", description = "Get one of the authenticated user's orders")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Order found",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Order not found",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun get(orderId: Long, @Parameter(hidden = true) userId: Long): OrderResponse

    @Operation(
        summary = "Upload payment proof",
        description = "Upload a bank transfer receipt (image or PDF). Only allowed while the order is PENDING_PAYMENT."
    )
    suspend fun uploadPaymentProof(orderId: Long, file: MultipartFile, @Parameter(hidden = true) userId: Long): OrderResponse
}
