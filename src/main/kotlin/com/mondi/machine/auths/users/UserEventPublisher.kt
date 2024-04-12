package com.mondi.machine.auths.users

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
@Component
class UserEventPublisher(private val applicationEventPublisher: ApplicationEventPublisher) {
  private val logger = LoggerFactory.getLogger(this::class.java)
  fun publish(payload: UserEventRequest) {
    logger.info("Starting to publish event : {}", payload)
    // -- setup the application event --
    val event = UserApplicationEvent(payload)
    // -- publish the event --
    applicationEventPublisher.publishEvent(event)
    logger.info("Finished to publish event : {}", payload)
  }
}