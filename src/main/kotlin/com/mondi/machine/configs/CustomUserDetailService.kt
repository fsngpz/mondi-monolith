package com.mondi.machine.configs

import com.mondi.machine.auths.users.UserService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component

/**
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Component
class CustomUserDetailService : UserDetailsService {
  lateinit var userService: UserService

  override fun loadUserByUsername(email: String): UserDetails {
    val userInfo = userService.getByEmail(email)
    return userInfo.map { CustomUserDetails() }
      .orElseThrow { UsernameNotFoundException("user not found $email") }
  }
}