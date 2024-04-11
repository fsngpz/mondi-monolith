package com.mondi.machine.accounts.profiles

import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
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
  var name: String,
) : AuditableBaseEntity<String>() {
  var profilePictureUrl: String? = null

  var address: String? = null
}