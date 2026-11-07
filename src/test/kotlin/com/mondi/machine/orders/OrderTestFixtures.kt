package com.mondi.machine.orders

import com.mondi.machine.accounts.addresses.Address
import com.mondi.machine.accounts.addresses.AddressTag
import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.auths.users.OAuthProvider
import com.mondi.machine.auths.users.User
import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductCategory
import com.mondi.machine.products.ProductStatus
import com.mondi.machine.utils.Currency
import java.math.BigDecimal

/**
 * Shared test data builders for order tests.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
internal object OrderTestFixtures {

    fun user(id: Long = 1L, email: String = "customer@example.com"): User {
        return User(email = email, password = "password", provider = OAuthProvider.LOCAL).apply {
            this.id = id
        }
    }

    fun profile(user: User = user()): Profile {
        return Profile(user).apply {
            this.id = user.id
            this.name = "Jane Doe"
        }
    }

    fun address(user: User = user(), id: Long = 5L): Address {
        return Address(
            user = user,
            recipientName = "Jane Doe",
            phone = "+6281234567890",
            addressLine1 = "Jl. Sudirman 1",
            addressLine2 = null,
            city = "Jakarta",
            state = "DKI Jakarta",
            postalCode = "10220",
            country = "Indonesia",
            tag = AddressTag.HOME,
            isMain = true
        ).apply { this.id = id }
    }

    fun product(
        id: Long = 1L,
        price: BigDecimal = BigDecimal("1000000"),
        discountPrice: BigDecimal = BigDecimal.ZERO,
        stock: Int = 10,
        status: ProductStatus = ProductStatus.ACTIVE
    ): Product {
        return Product(
            name = "Product $id",
            description = null,
            currency = Currency.IDR,
            specificationInHtml = null,
            price = price,
            discountPrice = discountPrice,
            category = ProductCategory.RING,
            stock = stock,
            sku = "RING_26_$id",
            status = status
        ).apply { this.id = id }
    }

    fun order(
        profile: Profile = profile(),
        status: OrderStatus = OrderStatus.PENDING_PAYMENT,
        id: Long = 10L
    ): Order {
        return Order(
            profile = profile,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            shippingRecipientName = "Jane Doe",
            shippingPhone = "+6281234567890",
            shippingAddressLine1 = "Jl. Sudirman 1",
            shippingAddressLine2 = null,
            shippingCity = "Jakarta",
            shippingState = "DKI Jakarta",
            shippingPostalCode = "10220",
            shippingCountry = "Indonesia",
            subtotal = BigDecimal("1000000"),
            totalPrice = BigDecimal("1000000")
        ).apply {
            this.id = id
            this.status = status
        }
    }

    fun Order.withItem(product: Product?, quantity: Int = 1, itemId: Long = 100L): Order {
        this.items.add(
            OrderItem(
                order = this,
                product = product,
                productName = product?.name ?: "Deleted product",
                productSku = product?.sku ?: "SKU",
                price = product?.price ?: BigDecimal.ONE,
                quantity = quantity
            ).apply { this.id = itemId }
        )
        return this
    }
}
