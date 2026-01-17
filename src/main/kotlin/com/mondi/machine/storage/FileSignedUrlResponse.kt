package com.mondi.machine.storage

import java.time.OffsetDateTime

/**
 * The model class of response for file signed URL.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-17
 */
data class FileSignedUrlResponse(
    val signedUrl: String,
    val expireAt: OffsetDateTime
)
