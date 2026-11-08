package com.mondi.machine.orders

import com.mondi.machine.products.Product
import java.math.BigDecimal

/**
 * an extension function to map the [Order] to [OrderResponse].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 * @return the [OrderResponse] instance.
 */
fun Order.toResponse(): OrderResponse {
    val id = requireNotNull(this.id) { "field id is null" }
    // -- map the customer, the profile can be null if it was deleted --
    val customer = this.profile?.let { profile ->
        OrderCustomerResponse(
            id = requireNotNull(profile.id) { "field profile id is null" },
            email = profile.user.email,
            name = profile.name,
            mobile = profile.user.mobile
        )
    }
    // -- return the mapped value --
    return OrderResponse(
        id = id,
        status = this.status,
        paymentMethod = this.paymentMethod,
        shippingAddress = OrderShippingAddressResponse(
            recipientName = this.shippingRecipientName,
            phone = this.shippingPhone,
            addressLine1 = this.shippingAddressLine1,
            addressLine2 = this.shippingAddressLine2,
            city = this.shippingCity,
            state = this.shippingState,
            postalCode = this.shippingPostalCode,
            country = this.shippingCountry
        ),
        subtotal = this.subtotal,
        discountAmount = this.discountAmount,
        totalPrice = this.totalPrice,
        voucherCode = this.voucherCode,
        paymentProofUrl = this.paymentProofUrl,
        notes = this.notes,
        shipment = this.shippingCourier?.let { courier ->
            OrderShipmentResponse(courier, this.trackingNumber, this.shippedAt)
        },
        items = this.items.map { it.toResponse() },
        customer = customer,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * an extension function to map the [OrderItem] to [OrderItemResponse].
 *
 * @return the [OrderItemResponse] instance.
 */
fun OrderItem.toResponse(): OrderItemResponse {
    val id = requireNotNull(this.id) { "field id is null" }
    // -- return the mapped value --
    return OrderItemResponse(
        id = id,
        productId = this.product?.id,
        productName = this.productName,
        productSku = this.productSku,
        price = this.price,
        quantity = this.quantity,
        certificateUrl = this.certificateUrl,
        mediaUrl = this.product?.media?.minByOrNull { it.displayOrder }?.mediaUrl
    )
}

/**
 * the price a customer pays for one unit of the [Product]: the discount price when it is a real
 * discount, otherwise the normal price. The backend stores `0` as discount price when there is none.
 *
 * @return the unit selling price.
 */
fun Product.sellingPrice(): BigDecimal {
    val discount = this.discountPrice
    return if (discount.signum() > 0 && discount < this.price) discount else this.price
}
