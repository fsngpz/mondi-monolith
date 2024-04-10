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
class CustomUserDetailService(private val userService: UserService) : UserDetailsService {
  override fun loadUserByUsername(email: String): UserDetails {
    val userInfo = userService.getByEmail(email)
    return userInfo.map { CustomUserDetails(it.email, it.password) }
      .orElseThrow { UsernameNotFoundException("user not found $email") }
  }
}