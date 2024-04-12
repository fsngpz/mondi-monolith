package com.mondi.machine.accounts.profiles

import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
@Component
class NewProfileEventListener(
  private val profileService: ProfileService
) : ApplicationListener<UserApplicationEvent> {
  private val logger = LoggerFactory.getLogger(this::class.java)
  override fun onApplicationEvent(event: UserApplicationEvent) {
    logger.info("Receiving the event with value: $event")
    // convert the source to instance of User --
    val eventResponse = event.source as UserEventRequest
    // -- get the user instance --
    val user = eventResponse.user
    // -- create the new profile --
    profileService.create(user)
    logger.info("Finished handle event with value: $event")
  }
}