package com.mondi.machine.backoffices.certificates

import com.mondi.machine.orders.BackofficeCertificateResponse
import com.mondi.machine.orders.CertificateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for the backoffice certificate list.
 * Certificates are uploaded per order item through the backoffice order API.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@Tag(name = "Backoffice Certificate API")
@RestController
@RequestMapping("/v1/backoffice/certificates")
class BackofficeCertificateController(private val service: CertificateService) {

    /**
     * a GET request to find all order items that have a certificate.
     *
     * @param search the search term (product name, SKU, order id, customer email or name).
     * @param pageable the [Pageable].
     * @return the [Page] of [BackofficeCertificateResponse].
     */
    @Operation(summary = "List certificates", description = "Paginated list of order items with a certificate")
    @GetMapping
    fun findAll(
        @RequestParam(required = false) search: String?,
        @PageableDefault(sort = ["updatedAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<BackofficeCertificateResponse> {
        // -- find all certificates --
        return service.findAll(search, pageable)
    }
}
