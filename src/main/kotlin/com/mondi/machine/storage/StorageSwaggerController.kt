package com.mondi.machine.storage

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * The interface for Storage Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Tag(name = "Storage API")
interface StorageSwaggerController {

    @Operation(summary = "Get signed URL for file download")
    suspend fun getSignedUrl(
        @Parameter(description = "File key or path", required = true) fileKey: String,
        @Parameter(description = "Expire duration in minutes") expireDurationInMinutes: Int? = null
    ): FileSignedUrlResponse
}
