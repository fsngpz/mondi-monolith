package com.mondi.machine.exceptions

/**
 * The exception class for invalid email verification token.
 *
 * This exception is thrown when an email verification token is expired, already used,
 * or otherwise invalid for verification purposes.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
class InvalidEmailVerificationTokenException(message: String) : RuntimeException(message)
