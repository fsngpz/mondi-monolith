package com.mondi.machine.auths.users

import java.util.Optional
import org.springframework.stereotype.Service

/**
 * The service class to handle business logic of [User].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@Service
class UserService(private val userRepository: UserRepository) {

  /**
   * a function to get the [User] by email.
   *
   * @param email the email address of user.
   * @return the Optional of [User] instance.
   */
  fun getByEmail(email: String): Optional<User> {
    return userRepository.findByEmail(email)
  }

  /**
   * a funciton returning boolean is the email already exist in database.
   *
   * @param email the email address.
   * @return the boolean of is email already exist.
   */
  fun isEmailAlreadyExist(email: String): Boolean {
    return getByEmail(email).isPresent
  }
}