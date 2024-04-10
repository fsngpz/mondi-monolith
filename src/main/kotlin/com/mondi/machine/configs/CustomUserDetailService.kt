package com.mondi.machine.configs

import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRepository
import java.util.Optional
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
  lateinit var userRepository: UserRepository

  override fun loadUserByUsername(email: String): UserDetails {
    val userInfo: Optional<User> = userRepository.findByEmail(email)
    return userInfo.map { CustomUserDetails() }
      .orElseThrow { UsernameNotFoundException("user not found $email") }
  }
}