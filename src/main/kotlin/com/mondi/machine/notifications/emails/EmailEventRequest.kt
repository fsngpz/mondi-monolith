package com.mondi.machine.notifications.emails

/**
 * The model class for email event request.
 *
 * @author Ferdinand Sangap.
 * @since 2026-02-09
 */
data class EmailEventRequest(
    val to: String,
    val subject: String,
    val templateName: String,
    val context: Map<String, Any>
)
