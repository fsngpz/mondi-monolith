package com.mondi.machine.auths.users

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The service class of User Registration.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Service
class UserRegistrationService(
  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder
) : UserService(userRepository) {

  /**
   * a function to create new [User].
   *
   * @param email the email address.
   * @param password the password.
   * @return the [User] instance.
   */
  @Transactional
  fun create(email: String, password: String): User {
    // -- validate is the email address already exist in database --
    require(!isEmailAlreadyExist(email)) {
      throw DataIntegrityViolationException("the email address '$email' is already exist in database")
    }
    // -- setup new instance of User --
    val newUser = User(
      email = email,
      password = passwordEncoder.encode(password),
      roles = ROLE_USER
    )
    // -- save the instance of user --
    return userRepository.save(newUser)
  }

  companion object {
    const val ROLE_USER = "USER"
  }
}