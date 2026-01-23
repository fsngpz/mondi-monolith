package com.mondi.machine.accounts.addresses

/**
 * Extension functions for Address entity.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */

/**
 * Convert Address entity to AddressResponse.
 *
 * @return the [AddressResponse] instance.
 */
fun Address.toResponse(): AddressResponse {
    val id = this.id
    // -- validate the field id --
    requireNotNull(id) {
        "value for 'id' is null"
    }
    // -- return the instance of AddressResponse --
    return AddressResponse(
        id = id,
        street = this.street,
        city = this.city,
        state = this.state,
        postalCode = this.postalCode,
        country = this.country,
        tag = this.tag,
        isMain = this.isMain,
        label = this.label,
        notes = this.notes
    )
}
