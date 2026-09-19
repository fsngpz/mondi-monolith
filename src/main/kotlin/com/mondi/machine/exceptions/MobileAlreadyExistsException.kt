package com.mondi.machine.exceptions

/**
 * Exception thrown when attempting to update a profile with a mobile number that is already in use by another user.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-31
 */
class MobileAlreadyExistsException(
    message: String = "Mobile number is already in use by another user"
) : RuntimeException(message)
