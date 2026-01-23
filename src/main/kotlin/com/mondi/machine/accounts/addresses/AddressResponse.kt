package com.mondi.machine.accounts.addresses

/**
 * The response model class for Address.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
data class AddressResponse(
    val id: Long,
    val street: String,
    val city: String,
    val state: String?,
    val postalCode: String?,
    val country: String,
    val tag: AddressTag,
    val isMain: Boolean,
    val label: String?,
    val notes: String?
)
