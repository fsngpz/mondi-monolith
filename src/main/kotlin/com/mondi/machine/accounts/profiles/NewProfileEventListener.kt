package com.mondi.machine.accounts.profiles

import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import com.mondi.machine.auths.users.UserService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * The new profile event listener.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
@Component
class NewProfileEventListener(
    private val profileService: ProfileService,
    private val userService: UserService
) : ApplicationListener<UserApplicationEvent> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * an override function to handle the [ApplicationListener] of [UserApplicationEvent].
     *
     * @param event the [UserApplicationEvent] instance.
     */
    override fun onApplicationEvent(event: UserApplicationEvent) {
        logger.info("Receiving the event with value: $event")
        // convert the source to instance of UserEventRequest --
        val eventResponse = event.source as UserEventRequest
        // -- get the user instance by ID --
        val user = userService.get(eventResponse.userId)
        val profilePictureUrl = eventResponse.profilePictureUrl
        // -- create the new profile --
        profileService.create(user, profilePictureUrl)
        logger.info("Finished handle event with value: $event")
    }
}
