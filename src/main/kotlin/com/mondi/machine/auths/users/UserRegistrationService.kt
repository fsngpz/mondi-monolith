package com.mondi.machine.auths.users

import com.mondi.machine.auths.roles.RoleService
import com.mondi.machine.auths.verification.EmailVerificationTokenService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

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
    private val userEventPublisher: UserEventPublisher,
    private val emailVerificationTokenService: EmailVerificationTokenService
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
     * This publishes the event after the transaction commits to avoid
     * Hibernate session conflicts with async listeners.
     *
     * @param user the [User] instance.
     */
    private fun sendEvent(user: User) {
        // -- generate verification token for new user --
        val token = emailVerificationTokenService.generateToken(user)
        // -- setup the instance of UserEventRequest --
        val eventRequest = UserEventRequest.from(user, verificationToken = token.token)

        // -- publish event after transaction commits --
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    userEventPublisher.publish(eventRequest)
                }
            })
        } else {
            userEventPublisher.publish(eventRequest)
        }
    }

    companion object {
        const val ROLE_USER = "ROLE_USER"
    }
}
