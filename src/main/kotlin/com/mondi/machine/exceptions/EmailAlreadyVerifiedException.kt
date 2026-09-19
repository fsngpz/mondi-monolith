package com.mondi.machine.exceptions

/**
 * The exception class for email already verified.
 *
 * This exception is thrown when attempting to resend verification email
 * to a user whose email address has already been verified.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-10
 */
class EmailAlreadyVerifiedException(message: String) : RuntimeException(message)
