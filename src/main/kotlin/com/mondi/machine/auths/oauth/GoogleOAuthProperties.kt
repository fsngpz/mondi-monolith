package com.mondi.machine.auths.oauth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * The configuration properties for Google OAuth.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Component
@ConfigurationProperties(prefix = "google.oauth")
data class GoogleOAuthProperties(
    var clientId: String = ""
)
