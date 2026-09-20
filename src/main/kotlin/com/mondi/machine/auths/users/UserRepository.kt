package com.mondi.machine.auths.users

import java.util.Optional
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * The interface for repository/database [User].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
interface UserRepository : JpaRepository<User, Long> {

  /**
   * a function to find the instance of [User] by id with profile eagerly fetched.
   *
   * @param id the user unique identifier.
   * @return the Optional of [User] with profile loaded.
   */
  @EntityGraph(attributePaths = ["profile"])
  @Query("SELECT u FROM User u WHERE u.id = :id")
  fun findByIdWithProfile(@Param("id") id: Long): Optional<User>

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

  /**
   * a function to find the instance of [User] by mobile number.
   *
   * @param mobile the mobile number.
   * @return the [User] instance or null.
   */
  fun findByMobile(mobile: String): User?
}