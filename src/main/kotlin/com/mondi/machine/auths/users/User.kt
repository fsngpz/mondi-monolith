package com.mondi.machine.auths.users

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The entity model class for table Userd.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "users")
class User(
  val email: String,
  val password: String,
) : AuditableBaseEntity<String>() {
  // -- optional --
  var username: String? = null

  // -- one to one --
  @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  var profile: Profile? = null

  // -- many to one --
  @OneToMany(mappedBy = "user")
  @OnDelete(action = OnDeleteAction.CASCADE)
  var roles: Set<UserRole> = setOf()
}