package com.mondi.machine.auths.users

import com.mondi.machine.auths.roles.Role
import org.springframework.data.jpa.repository.JpaRepository

/**
 * The repository interface of joined table [UserRole]
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
interface UserRoleRepository : JpaRepository<UserRole, Long> {
  /**
   * a function to find the instance [UserRole] by user and role.
   *
   * @param user the [User] instance.
   * @param role the [Role] instance.
   * @return the [UserRole] or  null.
   */
  fun findByUserAndRole(user: User, role: Role): UserRole?
}