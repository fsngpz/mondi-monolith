package com.mondi.machine.orders

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for the authenticated user's certificates.
 * Files are downloaded with `GET /v1/storage/file/signed-url?fileKey=<certificateUrl>`.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@Tag(name = "Certificate API")
@RestController
@RequestMapping("/v1/account/certificates")
class CertificateController(private val service: CertificateService) {

    /**
     * a GET request to find all certificates of the authenticated user.
     *
     * @param userId the user ID from request attribute.
     * @return the list of [CertificateResponse].
     */
    @Operation(
        summary = "List my certificates",
        description = "Certificates of the authenticated user's order items (newest first), then legacy certificates"
    )
    @GetMapping
    fun findAll(@Parameter(hidden = true) @RequestAttribute("ID") userId: Long): List<CertificateResponse> {
        // -- find all certificates --
        return service.findAllByUser(userId)
    }
}
