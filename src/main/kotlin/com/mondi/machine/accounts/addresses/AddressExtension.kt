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
        recipientName = this.recipientName,
        phone = this.phone,
        addressLine1 = this.addressLine1,
        addressLine2 = this.addressLine2,
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

/**
 * Convert Address entity to AddressRequest.
 *
 * @return the [AddressRequest] instance.
 */
fun Address.toRequest(): AddressRequest {
    return AddressRequest(
        recipientName = this.recipientName,
        phone = this.phone,
        addressLine1 = this.addressLine1,
        addressLine2 = this.addressLine2,
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

/**
 * a function to convert Address to AddressNullableRequest.
 *
 * @return the [AddressNullableRequest] instance.
 */
fun Address.toNullableRequest(): AddressNullableRequest {
    return AddressNullableRequest(
        recipientName = this.recipientName,
        phone = this.phone,
        addressLine1 = this.addressLine1,
        addressLine2 = this.addressLine2,
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

/**
 * an extension function to convert AddressNullableRequest to AddressRequest by validating non-nullable fields.
 *
 * @return the [AddressRequest] instance.
 */
fun AddressNullableRequest.toNonNull(): AddressRequest {
    require(!this.recipientName.isNullOrBlank()) {
        "field 'recipientName' cannot be null or blank"
    }
    require(!this.phone.isNullOrBlank()) {
        "field 'phone' cannot be null or blank"
    }
    require(!this.addressLine1.isNullOrBlank()) {
        "field 'addressLine1' cannot be null or blank"
    }
    require(!this.city.isNullOrBlank()) {
        "field 'city' cannot be null or blank"
    }
    require(!this.state.isNullOrBlank()) {
        "field 'state' cannot be null or blank"
    }
    require(!this.postalCode.isNullOrBlank()) {
        "field 'postalCode' cannot be null or blank"
    }
    require(!this.country.isNullOrBlank()) {
        "field 'country' cannot be null or blank"
    }
    require(this.tag != null) {
        "field 'tag' cannot be null"
    }
    require(this.isMain != null) {
        "field 'isMain' cannot be null"
    }
    return AddressRequest(
        recipientName = this.recipientName,
        phone = this.phone,
        addressLine1 = this.addressLine1,
        addressLine2 = this.addressLine2,
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
