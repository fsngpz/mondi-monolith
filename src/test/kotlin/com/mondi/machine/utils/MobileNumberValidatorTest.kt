package com.mondi.machine.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * The unit test class for [MobileNumberValidator].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
internal class MobileNumberValidatorTest {

    @Test
    fun `isValid returns true for valid international format with plus sign`() {
        // -- arrange --
        val mobile = "+1234567890"

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isTrue
    }

    @Test
    fun `isValid returns true for valid international format with spaces`() {
        // -- arrange --
        val mobile = "+1 234 567 890"

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isTrue
    }

    @Test
    fun `isValid returns true for valid format with parentheses and dashes`() {
        // -- arrange --
        val mobile = "(123) 456-7890"

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isTrue
    }

    @Test
    fun `isValid returns true for valid format with dashes only`() {
        // -- arrange --
        val mobile = "123-456-7890"

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isTrue
    }

    @Test
    fun `isValid returns true for minimum length 7 digits`() {
        // -- arrange --
        val mobile = "1234567"

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isTrue
    }

    @Test
    fun `isValid returns true for maximum length 15 digits`() {
        // -- arrange --
        val mobile = "+123456789012345"

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isTrue
    }

    @Test
    fun `isValid returns false for null input`() {
        // -- arrange --
        val mobile: String? = null

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isFalse
    }

    @Test
    fun `isValid returns false for blank input`() {
        // -- arrange --
        val mobile = "   "

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isFalse
    }

    @Test
    fun `isValid returns false for too short number`() {
        // -- arrange --
        val mobile = "123456"

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isFalse
    }

    @Test
    fun `isValid returns false for too long number`() {
        // -- arrange --
        val mobile = "+1234567890123456"

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isFalse
    }

    @Test
    fun `isValid returns false for non-numeric characters`() {
        // -- arrange --
        val mobile = "+123abc7890"

        // -- act --
        val result = MobileNumberValidator.isValid(mobile)

        // -- assert --
        assertThat(result).isFalse
    }

    @Test
    fun `normalize returns cleaned number for valid international format`() {
        // -- arrange --
        val mobile = "+1 (234) 567-890"

        // -- act --
        val result = MobileNumberValidator.normalize(mobile)

        // -- assert --
        assertThat(result).isEqualTo("+1234567890")
    }

    @Test
    fun `normalize returns cleaned number for local format`() {
        // -- arrange --
        val mobile = "(123) 456-7890"

        // -- act --
        val result = MobileNumberValidator.normalize(mobile)

        // -- assert --
        assertThat(result).isEqualTo("1234567890")
    }

    @Test
    fun `normalize returns null for invalid number`() {
        // -- arrange --
        val mobile = "123"

        // -- act --
        val result = MobileNumberValidator.normalize(mobile)

        // -- assert --
        assertThat(result).isNull()
    }

    @Test
    fun `validateAndNormalize returns null for null input`() {
        // -- arrange --
        val mobile: String? = null

        // -- act --
        val result = MobileNumberValidator.validateAndNormalize(mobile)

        // -- assert --
        assertThat(result).isNull()
    }

    @Test
    fun `validateAndNormalize returns null for blank input`() {
        // -- arrange --
        val mobile = "   "

        // -- act --
        val result = MobileNumberValidator.validateAndNormalize(mobile)

        // -- assert --
        assertThat(result).isNull()
    }

    @Test
    fun `validateAndNormalize returns normalized number for valid input`() {
        // -- arrange --
        val mobile = "+1 (234) 567-890"

        // -- act --
        val result = MobileNumberValidator.validateAndNormalize(mobile)

        // -- assert --
        assertThat(result).isEqualTo("+1234567890")
    }

    @Test
    fun `validateAndNormalize throws exception for invalid input`() {
        // -- arrange --
        val mobile = "123"

        // -- act & assert --
        val exception = assertThrows<IllegalArgumentException> {
            MobileNumberValidator.validateAndNormalize(mobile)
        }

        assertThat(exception.message).contains("Invalid mobile number format")
    }

    @Test
    fun `validateAndNormalize accepts various country formats`() {
        // -- US format --
        assertThat(MobileNumberValidator.validateAndNormalize("+1234567890")).isEqualTo("+1234567890")

        // -- UK format --
        assertThat(MobileNumberValidator.validateAndNormalize("+447700900123")).isEqualTo("+447700900123")

        // -- Indonesia format --
        assertThat(MobileNumberValidator.validateAndNormalize("+628123456789")).isEqualTo("+628123456789")

        // -- Singapore format --
        assertThat(MobileNumberValidator.validateAndNormalize("+6591234567")).isEqualTo("+6591234567")

        // -- Australia format --
        assertThat(MobileNumberValidator.validateAndNormalize("+61412345678")).isEqualTo("+61412345678")
    }
}
