package com.mondi.machine.configs

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
class CustomUserDetails : UserDetails {
  private lateinit var name: String
  private lateinit var password: String
  private lateinit var authorities: List<GrantedAuthority>
  override fun getAuthorities(): List<GrantedAuthority> {
    return this.authorities
  }

  override fun getPassword(): String {
    return this.password
  }

  override fun getUsername(): String {
    return this.name
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