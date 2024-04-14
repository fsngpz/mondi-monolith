package com.mondi.machine.auths.users

import com.mondi.machine.auths.roles.Role
import com.sun.jdi.request.DuplicateRequestException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * The business logic for [UserRole] features/
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Service
class UserRoleService(private val userRoleRepository: UserRoleRepository) {
  /**
   * a function to find all data with specified filter.
   *
   * @param search the parameter for filter data by email or username.
   * @param role the parameter to filter data by role.
   * @param pageable the [Pageable].
   * @return the [Page] of [UserRole].
   */
  fun findAll(search: String?, role: String?, pageable: Pageable): Page<UserRole> {
    return userRoleRepository.findAllCustom(search, role, pageable)
  }

  /**
   * a function to assign the [User] to single [Role].
   *
   * @param user the [User] instance.
   * @param role the [Role] instance.
   */
  fun assign(user: User, role: Role) {
    // -- check duplicate --
    checkDuplicate(user, role)
    // -- setup the instance UserRole --
    val userRole = UserRole(user, role)
    // -- save the instance to database --
    userRoleRepository.save(userRole)
  }

  /**
   * a private function to check is specified [User] and [Role] already exist in database.
   *
   * @param user the [User] instance.
   * @param role the [Role] instance.
   */
  private fun checkDuplicate(user: User, role: Role) {
    userRoleRepository.findByUserAndRole(user, role)?.let {
      throw DuplicateRequestException(
        "the user role for userId '${user.id}' and roleId '${role.id}' is already exist in database"
      )
    }
  }
}