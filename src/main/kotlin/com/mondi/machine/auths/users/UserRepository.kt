package com.mondi.machine.auths.users

import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository

/**
 * The interface for repository/database [User].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
interface UserRepository : JpaRepository<User, Long> {

  /**
   * a funtion to find the instance of [User] by email.
   *
   * @param email the email address.
   * @return the Optional of [User].
   */
  fun findByEmail(email: String): Optional<User>

  /**
   * a function to find the instance of [User] by provider and provider ID.
   *
   * @param provider the [OAuthProvider].
   * @param providerId the provider ID.
   * @return the [User] instance or null.
   */
  fun findByProviderAndProviderId(provider: OAuthProvider, providerId: String): User?
}