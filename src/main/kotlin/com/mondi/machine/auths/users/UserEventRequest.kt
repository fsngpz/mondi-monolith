package com.mondi.machine.auths.users

/**
 * The model class for event [User].
 *
 * This DTO contains only the necessary data to avoid passing managed entities
 * to async event listeners, which can cause Hibernate session conflicts.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
data class UserEventRequest(
    val userId: Long,
    val email: String,
    val userName: String,
    val isEmailVerified: Boolean,
    val verificationToken: String? = null,
    val profilePictureUrl: String? = null
) {
    companion object {
        /**
         * Create a UserEventRequest from a User entity.
         *
         * @param user the User entity.
         * @param verificationToken optional verification token string.
         * @param profilePictureUrl optional profile picture URL.
         * @return UserEventRequest with data extracted from the entity.
         */
        fun from(
            user: User,
            verificationToken: String? = null,
            profilePictureUrl: String? = null
        ): UserEventRequest {
            return UserEventRequest(
                userId = user.id ?: throw IllegalStateException("User ID cannot be null"),
                email = user.email,
                userName = user.profile?.name ?: user.email.substringBefore("@"),
                isEmailVerified = user.isEmailVerified,
                verificationToken = verificationToken,
                profilePictureUrl = profilePictureUrl
            )
        }
    }
}
