package com.mondi.machine.auths.roles

import com.mondi.machine.auths.users.UserRole
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The model class for table Role.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "roles")
class Role(
  val name: String
) : AuditableBaseEntity<String>() {
  // -- optional --
  var description: String? = name

  // -- one to many --
  @OneToMany(mappedBy = "role")
  @OnDelete(action = OnDeleteAction.CASCADE)
  var users: Set<UserRole> = setOf()
}