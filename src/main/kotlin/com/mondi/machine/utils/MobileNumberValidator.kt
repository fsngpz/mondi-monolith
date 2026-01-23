package com.mondi.machine.utils

/**
 * Utility object for validating mobile phone numbers from all countries.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
object MobileNumberValidator {

    /**
     * Validates international mobile phone numbers.
     *
     * Accepts formats:
     * - E.164 format: +1234567890 (with country code)
     * - International format: +1 234 567 890
     * - Local format with parentheses: (123) 456-7890
     * - Local format with dashes: 123-456-7890
     * - Local format with spaces: 123 456 7890
     *
     * Rules:
     * - Must start with + (optional) for international numbers
     * - Can contain digits, spaces, dashes, parentheses
     * - Minimum length: 7 digits (for local short numbers)
     * - Maximum length: 15 digits (E.164 standard)
     *
     * @param mobile the mobile number to validate.
     * @return true if valid, false otherwise.
     */
    fun isValid(mobile: String?): Boolean {
        if (mobile.isNullOrBlank()) {
            return false
        }

        // -- check if contains invalid characters (letters) --
        if (mobile.matches(Regex(".*[a-zA-Z].*"))) {
            return false
        }

        // -- remove all non-digit characters except plus sign --
        val digitsOnly = mobile.replace(Regex("[^+\\d]"), "")

        // -- check if it's empty after cleaning --
        if (digitsOnly.isEmpty()) {
            return false
        }

        // -- for international numbers (starting with +) --
        if (digitsOnly.startsWith("+")) {
            val digits = digitsOnly.substring(1)
            // -- must have 7-15 digits after the + sign --
            return digits.matches(Regex("^\\d{7,15}$"))
        }

        // -- for local numbers (no + sign) --
        // -- must have 7-15 digits --
        return digitsOnly.matches(Regex("^\\d{7,15}$"))
    }

    /**
     * Normalizes a mobile number to E.164 format if possible.
     * Removes all formatting characters except digits and leading +.
     *
     * @param mobile the mobile number to normalize.
     * @return normalized mobile number, or null if invalid.
     */
    fun normalize(mobile: String?): String? {
        if (!isValid(mobile)) {
            return null
        }

        return mobile?.replace(Regex("[^+\\d]"), "")
    }

    /**
     * Validates and normalizes a mobile number.
     *
     * @param mobile the mobile number to validate and normalize.
     * @return normalized mobile number.
     * @throws IllegalArgumentException if mobile number is invalid.
     */
    fun validateAndNormalize(mobile: String?): String? {
        // -- if mobile is null or blank, return null (optional field) --
        if (mobile.isNullOrBlank()) {
            return null
        }

        // -- validate the mobile number --
        require(isValid(mobile)) {
            "Invalid mobile number format. Please use international format (e.g., +1234567890) or local format (e.g., 123-456-7890)"
        }

        // -- return normalized mobile number --
        return normalize(mobile)
    }
}
