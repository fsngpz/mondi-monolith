package com.mondi.machine.utils

import com.mondi.machine.exceptions.TooManyRequestsException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

/**
 * The test class of [RateLimiterService].
 *
 * @author Ferdinand Sangap
 * @since 2026-02-10
 */
@SpringBootTest(classes = [RateLimiterService::class])
@TestPropertySource(
    properties = [
        "app.rate-limit.max-requests=3",
        "app.rate-limit.time-window-minutes=60"
    ]
)
internal class RateLimiterServiceTest(
    @Autowired private val service: RateLimiterService
) {

    @BeforeEach
    fun setUp() {
        // Clear any existing rate limits before each test
        service.clearRateLimit("test@example.com")
    }

    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
    }

    @Test
    fun `checkRateLimit allows requests within limit`() {
        // -- act & assert --
        // Should allow 3 requests
        service.checkRateLimit("test@example.com")
        service.checkRateLimit("test@example.com")
        service.checkRateLimit("test@example.com")

        // No exception thrown
    }

    @Test
    fun `checkRateLimit throws TooManyRequestsException when limit exceeded`() {
        // -- arrange --
        // Make 3 requests to reach the limit
        repeat(3) {
            service.checkRateLimit("test@example.com")
        }

        // -- act & assert --
        val exception = assertThrows<TooManyRequestsException> {
            service.checkRateLimit("test@example.com")
        }
        assertThat(exception.message).contains("Too many verification requests")
    }

    @Test
    fun `checkRateLimit tracks different identifiers separately`() {
        // -- act & assert --
        // Should allow 3 requests for each identifier
        repeat(3) {
            service.checkRateLimit("user1@example.com")
        }

        repeat(3) {
            service.checkRateLimit("user2@example.com")
        }

        // Both should reach limit
        assertThrows<TooManyRequestsException> {
            service.checkRateLimit("user1@example.com")
        }

        assertThrows<TooManyRequestsException> {
            service.checkRateLimit("user2@example.com")
        }
    }

    @Test
    fun `clearRateLimit removes all restrictions for identifier`() {
        // -- arrange --
        repeat(3) {
            service.checkRateLimit("test@example.com")
        }

        // Should throw on 4th request
        assertThrows<TooManyRequestsException> {
            service.checkRateLimit("test@example.com")
        }

        // -- act --
        service.clearRateLimit("test@example.com")

        // -- assert --
        // Should allow requests again
        service.checkRateLimit("test@example.com")
    }

    @Test
    fun `getRemainingRequests returns correct count`() {
        // -- act & assert --
        assertThat(service.getRemainingRequests("test@example.com")).isEqualTo(3)

        service.checkRateLimit("test@example.com")
        assertThat(service.getRemainingRequests("test@example.com")).isEqualTo(2)

        service.checkRateLimit("test@example.com")
        assertThat(service.getRemainingRequests("test@example.com")).isEqualTo(1)

        service.checkRateLimit("test@example.com")
        assertThat(service.getRemainingRequests("test@example.com")).isEqualTo(0)
    }

    @Test
    fun `getConfiguration returns configured values`() {
        // -- act --
        val (maxRequests, timeWindowMinutes) = service.getConfiguration()

        // -- assert --
        assertThat(maxRequests).isEqualTo(3)
        assertThat(timeWindowMinutes).isEqualTo(60)
    }

    @Test
    fun `rate limit exception includes time until reset`() {
        // -- arrange --
        repeat(3) {
            service.checkRateLimit("test@example.com")
        }

        // -- act & assert --
        val exception = assertThrows<TooManyRequestsException> {
            service.checkRateLimit("test@example.com")
        }

        assertThat(exception.message).containsAnyOf("minute", "minutes")
    }
}
