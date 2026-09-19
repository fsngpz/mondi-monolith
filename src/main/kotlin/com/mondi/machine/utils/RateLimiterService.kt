package com.mondi.machine.utils

import com.mondi.machine.exceptions.TooManyRequestsException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * The service class for rate limiting requests.
 *
 * This service provides a simple in-memory rate limiter that tracks
 * request timestamps per identifier (e.g., email address) and enforces
 * maximum request limits within a time window.
 *
 * @author Ferdinand Sangap
 * @since 2026-02-10
 */
@Service
class RateLimiterService(
    @Value("\${app.rate-limit.max-requests:3}") private val maxRequests: Int,
    @Value("\${app.rate-limit.time-window-minutes:60}") private val timeWindowMinutes: Long
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val requestTimestamps: ConcurrentHashMap<String, MutableList<OffsetDateTime>> = ConcurrentHashMap()

    /**
     * Check if a request is allowed for the given identifier.
     *
     * This method checks if the number of requests made within the time window
     * exceeds the maximum allowed requests. If exceeded, it throws TooManyRequestsException.
     *
     * @param identifier the unique identifier (e.g., email address).
     * @throws TooManyRequestsException if rate limit is exceeded.
     */
    fun checkRateLimit(identifier: String) {
        val now = OffsetDateTime.now()
        val windowStart = now.minusMinutes(timeWindowMinutes)

        // -- get or create request list for this identifier --
        val timestamps = requestTimestamps.getOrPut(identifier) { mutableListOf() }

        // -- remove timestamps outside the time window --
        synchronized(timestamps) {
            timestamps.removeIf { it.isBefore(windowStart) }

            // -- check if rate limit exceeded --
            if (timestamps.size >= maxRequests) {
                val oldestTimestamp = timestamps.minOrNull()
                val minutesUntilReset = if (oldestTimestamp != null) {
                    val resetTime = oldestTimestamp.plusMinutes(timeWindowMinutes)
                    val minutesLeft = java.time.Duration.between(now, resetTime).toMinutes()
                    if (minutesLeft > 0) minutesLeft else 1
                } else {
                    timeWindowMinutes
                }

                logger.warn("Rate limit exceeded for identifier: $identifier")
                throw TooManyRequestsException(
                    "Too many verification requests. Please try again in $minutesUntilReset minute(s)."
                )
            }

            // -- record this request --
            timestamps.add(now)
        }

        logger.debug("Rate limit check passed for identifier: $identifier (${timestamps.size}/$maxRequests)")
    }

    /**
     * Clear rate limit records for a specific identifier.
     *
     * This is useful for testing or when a user successfully completes an action.
     *
     * @param identifier the unique identifier to clear.
     */
    fun clearRateLimit(identifier: String) {
        requestTimestamps.remove(identifier)
        logger.debug("Rate limit cleared for identifier: $identifier")
    }

    /**
     * Scheduled task to clean up old timestamps.
     *
     * Runs every hour to remove expired timestamps and reduce memory usage.
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    fun cleanupExpiredTimestamps() {
        val now = OffsetDateTime.now()
        val windowStart = now.minusMinutes(timeWindowMinutes)
        var totalRemoved = 0

        requestTimestamps.forEach { (identifier, timestamps) ->
            synchronized(timestamps) {
                val sizeBefore = timestamps.size
                timestamps.removeIf { it.isBefore(windowStart) }
                totalRemoved += (sizeBefore - timestamps.size)

                // Remove empty lists
                if (timestamps.isEmpty()) {
                    requestTimestamps.remove(identifier)
                }
            }
        }

        if (totalRemoved > 0) {
            logger.info("Cleaned up $totalRemoved expired rate limit timestamps")
        }
    }

    /**
     * Get the current configuration values.
     *
     * @return Pair of (maxRequests, timeWindowMinutes).
     */
    fun getConfiguration(): Pair<Int, Long> {
        return Pair(maxRequests, timeWindowMinutes)
    }

    /**
     * Get the number of remaining requests for an identifier.
     *
     * @param identifier the unique identifier.
     * @return the number of remaining requests within the time window.
     */
    fun getRemainingRequests(identifier: String): Int {
        val now = OffsetDateTime.now()
        val windowStart = now.minusMinutes(timeWindowMinutes)

        val timestamps = requestTimestamps[identifier] ?: return maxRequests

        synchronized(timestamps) {
            // Clean up old timestamps
            timestamps.removeIf { it.isBefore(windowStart) }
            return maxRequests - timestamps.size
        }
    }
}
