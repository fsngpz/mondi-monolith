package com.mondi.machine.accounts.addresses

/**
 * The request model class for Address.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
data class AddressRequest(
    val recipientName: String,
    val phone: String,
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val tag: AddressTag,
    val isMain: Boolean,
    val label: String?,
    val notes: String?
)
