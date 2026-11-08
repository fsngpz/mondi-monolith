package com.mondi.machine.storage

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication

/**
 * The interface for Storage Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Tag(name = "Storage API")
interface StorageSwaggerController {

    @Operation(
        summary = "Get signed URL for file download",
        description = "Only for files stored on the caller's own records (profile picture, payment proofs, certificates); admins can open any file. Other keys return 404. Expiry is 1-60 minutes, default 15."
    )
    suspend fun getSignedUrl(
        @Parameter(description = "File key or path", required = true) fileKey: String,
        @Parameter(description = "Expire duration in minutes (1-60)") expireDurationInMinutes: Int? = null,
        @Parameter(hidden = true) userId: Long,
        @Parameter(hidden = true) authentication: Authentication?
    ): FileSignedUrlResponse
}
