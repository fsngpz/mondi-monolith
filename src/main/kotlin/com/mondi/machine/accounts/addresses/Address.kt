package com.mondi.machine.accounts.addresses

import com.mondi.machine.auths.users.User
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
 * The entity model class for user addresses.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "addresses")
class Address(
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    var street: String,
    var city: String,
    var state: String,
    var postalCode: String,
    var country: String,
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var tag: AddressTag = AddressTag.HOME,
    var isMain: Boolean = false
) : AuditableBaseEntity<String>() {
    // -- optional --
    var label: String? = null
    var notes: String? = null
}
