package com.mondi.machine.auths.roles

import org.springframework.stereotype.Service

/**
 * The service class for business logic [Role].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Service
class RoleService(private val roleRepository: RoleRepository) {

  /**
   * a function to get or create instance [Role].
   *
   * @param name the name of role.
   * @param description the description of role.
   * @return the [Role].
   */
  fun getOrCreate(name: String, description: String? = null): Role {
    // -- find the role with specified name in database --
    val role = roleRepository.findByNameIgnoreCase(name) ?: Role(name).apply {
      this.description = description
    }
    // -- save the instance to repository --
    return roleRepository.save(role)
  }
}