package com.mondi.machine.accounts.addresses

import com.mondi.machine.exceptions.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody

/**
 * The Swagger documentation interface for Address operations.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@Tag(name = "Address API")
interface AddressSwaggerController {

    @Operation(summary = "Get all addresses", description = "Get all addresses for the authenticated user")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved addresses",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = AddressResponse::class))
                    )
                ]
            )
        ]
    )
    @GetMapping
    fun getAll(@RequestAttribute("ID") userId: Long): ResponseEntity<List<AddressResponse>>

    @Operation(summary = "Get address by ID", description = "Get a specific address by ID")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved address",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AddressResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Address not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @GetMapping("/{addressId}")
    fun getById(
        @PathVariable addressId: Long,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<AddressResponse>

    @Operation(summary = "Get main address", description = "Get the main address for the authenticated user")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved main address",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AddressResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Main address not found"
            )
        ]
    )
    @GetMapping("/main")
    fun getMain(@RequestAttribute("ID") userId: Long): ResponseEntity<AddressResponse>

    @Operation(summary = "Create address", description = "Create a new address for the authenticated user")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Successfully created address",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AddressResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @PostMapping
    fun create(
        @RequestBody request: AddressRequest,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<AddressResponse>

    @Operation(summary = "Update address", description = "Update an existing address")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully updated address",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AddressResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Address not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @PutMapping("/{addressId}")
    fun update(
        @PathVariable addressId: Long,
        @RequestBody request: AddressRequest,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<AddressResponse>

    @Operation(summary = "Set as main address", description = "Set an address as the main address for the user")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully set as main address",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AddressResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Address not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @PatchMapping("/{addressId}/set-main")
    fun setAsMain(
        @PathVariable addressId: Long,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<AddressResponse>

    @Operation(summary = "Delete address", description = "Delete an address")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Successfully deleted address"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Address not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Cannot delete main address",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @DeleteMapping("/{addressId}")
    fun delete(
        @PathVariable addressId: Long,
        @RequestAttribute("ID") userId: Long
    ): ResponseEntity<Void>
}
