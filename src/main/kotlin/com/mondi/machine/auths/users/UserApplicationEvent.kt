package com.mondi.machine.auths.users

import org.springframework.context.ApplicationEvent

/**
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
class UserApplicationEvent(payload: UserEventRequest) : ApplicationEvent(payload)