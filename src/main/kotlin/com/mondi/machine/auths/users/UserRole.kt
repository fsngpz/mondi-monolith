package com.mondi.machine.auths.users

import com.mondi.machine.auths.roles.Role
import com.mondi.machine.utils.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * The entity class of join table for [User] and [Role].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Entity
@Table(name = "users_roles")
class UserRole(
  @ManyToOne
  @JoinColumn(name = "user_id")
  val user: User,
  @ManyToOne
  @JoinColumn(name = "role_id")
  val role: Role
) : BaseEntity()