package com.mondi.machine.configs

import com.mondi.machine.auths.roles.Role
import com.mondi.machine.auths.users.UserRole
import com.mondi.machine.auths.users.UserService
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * The custom class extending the [UserDetailsService].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Component
class CustomUserDetailService(private val userService: UserService) : UserDetailsService {
  @Transactional
  override fun loadUserByUsername(email: String): UserDetails {
    val userInfo = userService.findByEmail(email)
    return userInfo.map { CustomUserDetails(it) }
      .orElseThrow { UsernameNotFoundException("user not found $email") }
  }

  /**
   * a function to get the [CustomUserDetails] instance.
   *
   * @param email the email address as the unique identifier.
   * @return the [CustomUserDetails] instance.
   */
  fun getCustomUserDetails(email: String): CustomUserDetails {
    // -- get the user instance by id --
    val user = userService.getByEmail(email)
    // -- return CustomUserDetails using User constructor --
    return CustomUserDetails(user)
  }

  /**
   * a private function to convert the List of [Role] to List of [GrantedAuthority].
   *
   * @return the List of [GrantedAuthority].
   */
  private fun Set<UserRole>.toGrantedAuthorities(): List<GrantedAuthority> {
    return this.map { SimpleGrantedAuthority(it.role.name) }
  }
}