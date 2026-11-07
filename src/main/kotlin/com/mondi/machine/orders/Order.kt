package com.mondi.machine.orders

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The entity class for a customer Order placed through checkout.
 *
 * The entity name is `CustomerOrder` because `Order` is a reserved word in JPQL.
 * The shipping address is copied from the customer's [com.mondi.machine.accounts.addresses.Address]
 * when the order is placed, so later address edits do not change past orders.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@Entity(name = "CustomerOrder")
@EntityListeners(AuditingEntityListener::class)
@Table(name = "orders")
class Order(
    // -- nullable: the column is set to null if the profile is deleted --
    @ManyToOne
    @JoinColumn(name = "profile_id")
    val profile: Profile?,

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var paymentMethod: PaymentMethod,

    // -- shipping address snapshot --
    var shippingRecipientName: String,
    var shippingPhone: String,
    var shippingAddressLine1: String,
    var shippingAddressLine2: String?,
    var shippingCity: String,
    var shippingState: String,
    var shippingPostalCode: String,
    var shippingCountry: String,

    // -- amounts --
    var subtotal: BigDecimal,
    var discountAmount: BigDecimal = BigDecimal.ZERO,
    var totalPrice: BigDecimal
) : AuditableBaseEntity<String>() {

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var status: OrderStatus = OrderStatus.PENDING_PAYMENT

    // -- optional --
    var voucherCode: String? = null
    var paymentProofUrl: String? = null
    var notes: String? = null

    // -- shipment, set from the backoffice --
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var shippingCourier: ShippingCourier? = null
    var trackingNumber: String? = null
    var shippedAt: OffsetDateTime? = null

    // -- One to Many --
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    val items: MutableList<OrderItem> = mutableListOf()
}
