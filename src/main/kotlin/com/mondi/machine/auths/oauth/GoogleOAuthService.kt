package com.mondi.machine.auths.oauth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.mondi.machine.auths.AuthenticationResponse
import com.mondi.machine.auths.jwt.JwtService
import com.mondi.machine.auths.refresh.RefreshTokenService
import com.mondi.machine.auths.roles.RoleService
import com.mondi.machine.auths.users.OAuthProvider
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserEventPublisher
import com.mondi.machine.auths.users.UserEventRequest
import com.mondi.machine.auths.users.UserRepository
import com.mondi.machine.auths.users.UserRoleService
import com.mondi.machine.auths.users.UserService
import com.mondi.machine.configs.CustomUserDetails
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Collections

/**
 * The service class for Google OAuth authentication.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Service
class GoogleOAuthService(
    private val googleOAuthProperties: GoogleOAuthProperties,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: UserRepository,
    private val roleService: RoleService,
    private val userRoleService: UserRoleService,
    private val userEventPublisher: UserEventPublisher
) : UserService(userRepository) {

    private val logger: Logger = LoggerFactory.getLogger(GoogleOAuthService::class.java)

    /**
     * a function to authenticate user with Google OAuth.
     *
     * @param idToken the Google ID token from client.
     * @return the [AuthenticationResponse] containing JWT token and refresh token.
     * @throws IllegalArgumentException if token is invalid.
     */
    @Transactional
    fun authenticate(idToken: String): AuthenticationResponse {
        // -- verify the Google ID token --
        val payload = verifyGoogleToken(idToken)
            ?: throw IllegalArgumentException("Invalid Google ID token")

        // -- extract user information from token payload --
        val email = payload.email
        val providerId = payload.subject
        val name = payload["name"] as? String
        val profilePictureUrl = payload["picture"] as? String

        // -- find or create user --
        val user = findOrCreateOAuthUser(email, providerId, name, profilePictureUrl)

        // -- generate JWT access token --
        val customUserDetails = CustomUserDetails(user)
        val bearerToken = jwtService.generateToken(customUserDetails)

        // -- generate refresh token --
        val refreshToken = refreshTokenService.generateRefreshToken(user)

        // -- return authentication response --
        return AuthenticationResponse(bearerToken, refreshToken)
    }

    /**
     * a function to verify Google ID token.
     *
     * @param idToken the Google ID token string.
     * @return the [GoogleIdToken.Payload] if valid, null otherwise.
     */
    internal fun verifyGoogleToken(idToken: String): GoogleIdToken.Payload? {
        return try {
            // -- create verifier with SKIP_AUDIENCE verification for development --
            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
                .setAudience(Collections.singletonList(googleOAuthProperties.clientId))
                .build()
            // -- verify token --
            val googleIdToken = verifier.verify(idToken)
            googleIdToken.payload
        } catch (e: Exception) {
            logger.error("Exception during token verification: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * a function to find existing OAuth user or create new one.
     *
     * @param email the user email.
     * @param providerId the provider ID (Google user ID).
     * @param name the user name from Google profile.
     * @param profilePictureUrl the profile picture URL.
     * @return the [User] instance.
     */
    private fun findOrCreateOAuthUser(
        email: String,
        providerId: String,
        name: String?,
        profilePictureUrl: String?
    ): User {
        // -- try to find user by provider and provider ID --
        val existingUser = userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, providerId)

        if (existingUser != null) {
            return existingUser
        }

        // -- check if user with same email exists (local account) --
        require(!isEmailAlreadyExist(email)) {
            "An account with email '$email' already exists. Please login with your password."
        }

        // -- create new OAuth user --
        val newUser = User(
            email = email,
            password = null,
            provider = OAuthProvider.GOOGLE,
            providerId = providerId
        )

        // -- set username from Google profile --
        newUser.username = name

        // -- save the user --
        userRepository.save(newUser)

        // -- assign default role --
        val role = roleService.getOrCreate(ROLE_USER)
        userRoleService.assign(newUser, role)

        // -- publish event for profile creation --
        sendEvent(newUser, profilePictureUrl)

        // -- return the new user --
        return newUser
    }

    /**
     * a private function to publish event.
     *
     * @param user the [User] instance.
     * @param profilePictureUrl the profile picture URL.
     */
    private fun sendEvent(user: User, profilePictureUrl: String?) {
        // -- setup the instance of UserEventRequest --
        val eventRequest = UserEventRequest(user, profilePictureUrl)
        // -- publish the event --
        userEventPublisher.publish(eventRequest)
    }

    companion object {
        const val ROLE_USER = "ROLE_USER"
    }
}
