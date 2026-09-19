package com.mondi.machine.auths.verification

import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import com.mondi.machine.auths.users.UserService
import com.mondi.machine.exceptions.EmailAlreadyVerifiedException
import com.mondi.machine.exceptions.EmailVerificationTokenNotFoundException
import com.mondi.machine.utils.RateLimiterService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The service class for resending email verification.
 *
 * This service handles the logic for resending verification emails,
 * including rate limiting, user validation, and event publishing.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-10
 */
@Service
class ResendVerificationService(
    private val userService: UserService,
    private val emailVerificationTokenService: EmailVerificationTokenService,
    private val rateLimiterService: RateLimiterService,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Resend verification email to the specified email address.
     *
     * This method:
     * 1. Checks rate limit for the email
     * 2. Validates that the user exists
     * 3. Checks that the email is not already verified
     * 4. Generates a new verification token
     * 5. Publishes an event to send the verification email asynchronously
     *
     * @param userId the user unique identifier.
     * @return [ResendVerificationResponse] with confirmation message.
     * @throws EmailVerificationTokenNotFoundException if user not found.
     * @throws EmailAlreadyVerifiedException if email is already verified.
     * @throws com.mondi.machine.exceptions.TooManyRequestsException if rate limit is exceeded.
     */
    @Transactional
    fun resendVerification(userId: Long): ResendVerificationResponse {
        // -- get the user by id --
        val user = userService.get(userId)
        val email = user.email

        logger.info("Attempting to resend verification email to: $email")

        // -- check rate limit --
        rateLimiterService.checkRateLimit(email)

        // -- check if already verified --
        if (user.isEmailVerified) {
            logger.warn("Email already verified for: $email")
            throw EmailAlreadyVerifiedException("Email address is already verified")
        }

        // -- generate new verification token --
        emailVerificationTokenService.generateToken(user)
        logger.info("New verification token generated for: $email")

        // -- initialize lazy-loaded associations before async processing --
        // This ensures the profile is loaded in the current transaction
        // before passing the user entity to async event listeners
        user.profile?.name

        // -- publish event to send email asynchronously --
        val userEventRequest = UserEventRequest(user)
        val event = UserApplicationEvent(userEventRequest)
        applicationEventPublisher.publishEvent(event)
        logger.info("Email verification event published for: $email")

        return ResendVerificationResponse(
            message = "Verification email has been sent. Please check your inbox.",
            email = email,
            expiresInHours = emailVerificationTokenService.getTokenExpiryHours()
        )
    }
}
