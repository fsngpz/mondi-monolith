package com.mondi.machine.vouchers

import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The entity class for a discount Voucher code, managed from the backoffice.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "vouchers")
class Voucher(
    // -- stored upper case --
    var code: String,
    var description: String?,
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var type: VoucherType,
    var value: BigDecimal,
    var maxDiscountAmount: BigDecimal?,
    var minOrderAmount: BigDecimal = BigDecimal.ZERO,
    // -- null means unlimited --
    var maxUsage: Int?,
    var startDate: OffsetDateTime,
    var endDate: OffsetDateTime,
    @Column(name = "is_active")
    var isActive: Boolean = true
) : AuditableBaseEntity<String>() {
    var currentUsage: Int = 0
}
