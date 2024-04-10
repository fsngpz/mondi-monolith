package com.mondi.machine.auths.users

import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
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
  val roles: String
) : AuditableBaseEntity<String>() {
  var username: String? = null
}