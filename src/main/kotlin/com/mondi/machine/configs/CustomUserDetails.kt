package com.mondi.machine.configs

import com.mondi.machine.auths.users.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * The custom class for User Details.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
class CustomUserDetails(
  private val id: Long,
  private val email: String,
  private val password: String?,
  private var authorities: List<GrantedAuthority>
) : UserDetails {

  /**
   * Constructor that accepts a User entity.
   *
   * @param user the [User] entity.
   */
  constructor(user: User) : this(
    requireNotNull(user.id) { "User ID cannot be null" },
    user.email,
    user.password,
    user.roles.map { SimpleGrantedAuthority(it.role.name) }
  )
  fun getId(): Long {
    return this.id
  }

  override fun getAuthorities(): List<GrantedAuthority> {
    return this.authorities
  }

  override fun getPassword(): String? {
    return this.password
  }

  override fun getUsername(): String {
    return this.email
  }

  override fun isAccountNonExpired(): Boolean {
    return true
  }

  override fun isAccountNonLocked(): Boolean {
    return true
  }

  override fun isCredentialsNonExpired(): Boolean {
    return true
  }

  override fun isEnabled(): Boolean {
    return true
  }
}