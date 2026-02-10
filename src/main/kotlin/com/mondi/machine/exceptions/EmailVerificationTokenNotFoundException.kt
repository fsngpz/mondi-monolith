package com.mondi.machine.exceptions

/**
 * The exception class for email verification token not found.
 *
 * This exception is thrown when a requested email verification token cannot be found in the database.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
class EmailVerificationTokenNotFoundException(message: String) : RuntimeException(message)
