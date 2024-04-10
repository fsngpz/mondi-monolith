package com.mondi.machine.configs

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
class CustomUserDetails(
  private val email: String,
  private val password: String,
  private var authorities: List<GrantedAuthority> = listOf()
) : UserDetails {
  override fun getAuthorities(): List<GrantedAuthority> {
    return this.authorities
  }

  override fun getPassword(): String {
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