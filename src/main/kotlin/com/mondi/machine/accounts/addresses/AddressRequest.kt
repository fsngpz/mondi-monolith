package com.mondi.machine.accounts.addresses

/**
 * The request model class for Address.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
data class AddressRequest(
    val street: String,
    val city: String,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String,
    val tag: AddressTag = AddressTag.HOME,
    val isMain: Boolean = false,
    val label: String? = null,
    val notes: String? = null
)
