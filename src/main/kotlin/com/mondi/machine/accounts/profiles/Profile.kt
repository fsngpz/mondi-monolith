package com.mondi.machine.accounts.profiles

import com.mondi.machine.auths.users.User
import com.mondi.machine.transactions.Transaction
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The model class for Profile Account.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "profiles")
class Profile(

    // -- one to one --
    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    val user: User
) : AuditableBaseEntity<String>() {
    // -- optional --
    var name: String? = null
    var profilePictureUrl: String? = null

    // -- One to Many --
    @OneToMany(mappedBy = "profile")
    @OnDelete(action = OnDeleteAction.CASCADE)
    var transactions: Set<Transaction> = setOf()
}
