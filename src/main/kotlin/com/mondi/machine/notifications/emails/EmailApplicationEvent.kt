package com.mondi.machine.notifications.emails

import org.springframework.context.ApplicationEvent

/**
 * The event class for email notifications.
 *
 * @author Ferdinand Sangap.
 * @since 2026-02-09
 */
class EmailApplicationEvent(payload: EmailEventRequest) : ApplicationEvent(payload)
