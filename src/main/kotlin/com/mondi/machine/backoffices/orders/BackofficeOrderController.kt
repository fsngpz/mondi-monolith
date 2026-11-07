package com.mondi.machine.backoffices.orders

import com.mondi.machine.orders.OrderResponse
import com.mondi.machine.orders.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * The REST controller for backoffice order management.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@RestController
@RequestMapping("/v1/backoffice/orders")
class BackofficeOrderController(private val service: BackofficeOrderService) : BackofficeOrderSwaggerController {

    /**
     * a GET request to find all orders.
     *
     * @param search the search term.
     * @param status the status to filter by.
     * @param pageable the [Pageable].
     * @return the [Page] of [OrderResponse].
     */
    @GetMapping
    override fun findAll(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: OrderStatus?,
        @PageableDefault(sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<OrderResponse> {
        // -- find all orders --
        return service.findAll(search, status, pageable)
    }

    /**
     * a GET request to get an order.
     *
     * @param orderId the order unique identifier.
     * @return the [OrderResponse].
     */
    @GetMapping("/{orderId}")
    override fun get(@PathVariable orderId: Long): OrderResponse {
        // -- get the order --
        return service.get(orderId)
    }

    /**
     * a PATCH request to change the status of an order.
     *
     * @param orderId the order unique identifier.
     * @param request the [BackofficeOrderStatusRequest] instance.
     * @return the updated [OrderResponse].
     */
    @PatchMapping("/{orderId}/status")
    override fun updateStatus(
        @PathVariable orderId: Long,
        @RequestBody request: BackofficeOrderStatusRequest
    ): OrderResponse {
        // -- update the status --
        return service.updateStatus(orderId, request.status)
    }

    /**
     * a POST request to upload the certificate of an order item.
     *
     * @param orderId the order unique identifier.
     * @param itemId the order item unique identifier.
     * @param file the certificate file.
     * @return the updated [OrderResponse].
     */
    @PostMapping("/{orderId}/items/{itemId}/certificate", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override suspend fun uploadCertificate(
        @PathVariable orderId: Long,
        @PathVariable itemId: Long,
        @RequestPart("file") file: MultipartFile
    ): OrderResponse {
        // -- upload the certificate --
        return service.uploadCertificate(orderId, itemId, file)
    }
}
