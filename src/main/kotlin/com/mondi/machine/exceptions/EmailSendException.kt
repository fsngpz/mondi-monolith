package com.mondi.machine.exceptions

/**
 * Custom exception for email sending failures.
 *
 * This exception is thrown when an email fails to send due to SMTP errors,
 * template rendering issues, or other email-related problems.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-31
 */
class EmailSendException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
