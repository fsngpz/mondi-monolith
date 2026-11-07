package com.mondi.machine.loyalty

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The entity class for one change to a customer's loyalty points. The balance is the sum.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "loyalty_points")
class LoyaltyPoint(
    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,
    // -- positive to add, negative to take away --
    val points: Int,
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    val transactionType: LoyaltyTransactionType,
    val orderId: Long?,
    val description: String?
) : AuditableBaseEntity<String>()
