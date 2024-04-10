package com.mondi.machine.auths.roles

import org.springframework.data.jpa.repository.JpaRepository

/**
 * The interface for [Role] database.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
interface RoleRepository : JpaRepository<Role, Long> {
  /**
   * a function to find the [Role] by name with case-insensitive.
   *
   * @param name the name of role.
   * @return the [Role] or null.
   */
  fun findByNameIgnoreCase(name: String): Role?
}