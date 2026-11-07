package com.mondi.machine.orders

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * The REST controller for customer [Order] operations.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@RestController
@RequestMapping("/v1/orders")
class OrderController(private val service: OrderService) : OrderSwaggerController {

    /**
     * a POST request to place a new order.
     *
     * @param request the [CreateOrderRequest] instance.
     * @param userId the user ID from request attribute.
     * @return the created [OrderResponse].
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(
        @RequestBody request: CreateOrderRequest,
        @RequestAttribute("ID") userId: Long
    ): OrderResponse {
        // -- create the order --
        return service.create(userId, request)
    }

    /**
     * a GET request to find all orders of the authenticated user.
     *
     * @param status the status to filter by.
     * @param pageable the [Pageable].
     * @param userId the user ID from request attribute.
     * @return the [Page] of [OrderResponse].
     */
    @GetMapping
    override fun findAll(
        @RequestParam(required = false) status: OrderStatus?,
        @PageableDefault(sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
        @RequestAttribute("ID") userId: Long
    ): Page<OrderResponse> {
        // -- find all orders --
        return service.findAll(userId, status, pageable)
    }

    /**
     * a GET request to get an order of the authenticated user.
     *
     * @param orderId the order unique identifier.
     * @param userId the user ID from request attribute.
     * @return the [OrderResponse].
     */
    @GetMapping("/{orderId}")
    override fun get(
        @PathVariable orderId: Long,
        @RequestAttribute("ID") userId: Long
    ): OrderResponse {
        // -- get the order --
        return service.get(orderId, userId)
    }

    /**
     * a POST request to upload the payment proof of an order.
     *
     * @param orderId the order unique identifier.
     * @param file the payment proof file.
     * @param userId the user ID from request attribute.
     * @return the updated [OrderResponse].
     */
    @PostMapping("/{orderId}/payment-proof", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override suspend fun uploadPaymentProof(
        @PathVariable orderId: Long,
        @RequestPart("file") file: MultipartFile,
        @RequestAttribute("ID") userId: Long
    ): OrderResponse {
        // -- upload the payment proof --
        return service.uploadPaymentProof(orderId, userId, file)
    }
}
