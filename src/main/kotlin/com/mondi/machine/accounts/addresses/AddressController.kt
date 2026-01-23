package com.mondi.machine.accounts.addresses

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for Address operations.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@RestController
@RequestMapping("/v1/account/addresses")
class AddressController(
    private val addressService: AddressService
) : AddressSwaggerController {

    /**
     * Get all addresses for the authenticated user.
     *
     * @param userId the user ID from request attribute.
     * @return list of [AddressResponse].
     */
    @GetMapping
    override fun getAll(
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<List<AddressResponse>> {
        // -- get all addresses --
        val addresses = addressService.getAllByUserId(userId)
        // -- convert to response --
        val response = addresses.map { it.toResponse() }
        return ResponseEntity.ok(response)
    }

    /**
     * Get a specific address by ID.
     *
     * @param addressId the address ID.
     * @param userId the user ID from request attribute.
     * @return the [AddressResponse].
     */
    @GetMapping("/{addressId}")
    override fun getById(
        @PathVariable addressId: Long,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<AddressResponse> {
        // -- get address by id --
        val address = addressService.getById(addressId, userId)
        // -- convert to response --
        return ResponseEntity.ok(address.toResponse())
    }

    /**
     * Get the main address for the authenticated user.
     *
     * @param userId the user ID from request attribute.
     * @return the main [AddressResponse] or 404 if not found.
     */
    @GetMapping("/main")
    override fun getMain(
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<AddressResponse> {
        // -- get main address --
        val address = addressService.getMainAddress(userId)
            ?: return ResponseEntity.notFound().build()
        // -- convert to response --
        return ResponseEntity.ok(address.toResponse())
    }

    /**
     * Create a new address.
     *
     * @param request the [AddressRequest] instance.
     * @param userId the user ID from request attribute.
     * @return the created [AddressResponse].
     */
    @PostMapping
    override fun create(
        @RequestBody request: AddressRequest,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<AddressResponse> {
        // -- create new address --
        val address = addressService.create(userId, request)
        // -- convert to response --
        return ResponseEntity.status(HttpStatus.CREATED).body(address.toResponse())
    }

    /**
     * Patch / partial update an existing address.
     *
     * @param addressId the address ID.
     * @param request the [AddressRequest] instance.
     * @param userId the user ID from request attribute.
     * @return the updated [AddressResponse].
     */
    @PatchMapping("/{addressId}")
    override fun patch(
        @PathVariable addressId: Long,
        @RequestBody request: JsonNode,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<AddressResponse> {
        // -- patch address --
        val address = addressService.patch(addressId, userId, request)
        // -- convert to response --
        return ResponseEntity.ok(address.toResponse())
    }

    /**
     * Set an address as the main address.
     *
     * @param addressId the address ID.
     * @param userId the user ID from request attribute.
     * @return the updated [AddressResponse].
     */
    @PatchMapping("/{addressId}/main")
    override fun setAsMain(
        @PathVariable addressId: Long,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<AddressResponse> {
        // -- set as main address --
        val address = addressService.setAsMain(addressId, userId)
        // -- convert to response --
        return ResponseEntity.ok(address.toResponse())
    }

    /**
     * Delete an address.
     *
     * @param addressId the address ID.
     * @param userId the user ID from request attribute.
     * @return no content response.
     */
    @DeleteMapping("/{addressId}")
    override fun delete(
        @PathVariable addressId: Long,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<Void> {
        // -- delete address --
        addressService.delete(addressId, userId)
        // -- return no content --
        return ResponseEntity.noContent().build()
    }
}
