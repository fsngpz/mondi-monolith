package com.mondi.machine.accounts.addresses

/**
 * The request model class for Address with all nullable fields.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-23
 */
data class AddressNullableRequest(
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val tag: AddressTag? = null,
    val isMain: Boolean? = null,
    val label: String? = null,
    val notes: String? = null
)
