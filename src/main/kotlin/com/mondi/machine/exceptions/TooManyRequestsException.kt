package com.mondi.machine.exceptions

/**
 * The exception class for rate limit exceeded.
 *
 * This exception is thrown when a client makes too many requests
 * within a specified time period, exceeding the rate limit.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-10
 */
class TooManyRequestsException(message: String) : RuntimeException(message)
