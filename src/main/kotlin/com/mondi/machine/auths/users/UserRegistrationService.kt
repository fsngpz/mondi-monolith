package com.mondi.machine.auths.users

import com.mondi.machine.auths.roles.RoleService
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
  private val roleService: RoleService,
  private val userRoleService: UserRoleService,
  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder,
  private val userEventPublisher: UserEventPublisher
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
      password = passwordEncoder.encode(password)
    )
    // -- save the instance of user --
    userRepository.save(newUser)
    // -- setup the instance of Role --
    val role = roleService.getOrCreate(ROLE_USER)
    // -- assign user to the role --
    userRoleService.assign(newUser, role)
    // -- publish event --
    sendEvent(newUser)
    // -- return the instance of newUser --
    return newUser
  }

  /**
   * a private function to publish event.
   *
   * @param user the [User] instance.
   */
  private fun sendEvent(user: User) {
    // -- setup the instance of UserEventRequest --
    val eventRequest = UserEventRequest(user)
    // -- publish the event --
    userEventPublisher.publish(eventRequest)
  }

  companion object {
    const val ROLE_USER = "ROLE_USER"
  }
}