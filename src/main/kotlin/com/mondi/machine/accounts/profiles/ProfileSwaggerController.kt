package com.mondi.machine.accounts.profiles

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType

/**
 * The interface for Profile Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Tag(name = "Profile API")
interface ProfileSwaggerController {

    @Operation(summary = "Get user profile")
    suspend fun get(@Parameter(description = "User ID", hidden = true) id: Long): ProfileResponse

    @Operation(
        summary = "Update user profile",
        requestBody = RequestBody(
            content = [Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = Schema(implementation = ProfileFileRequest::class)
            )]
        )
    )
    suspend fun patch(
        payload: ProfileFileRequest,
        @Parameter(description = "User ID", hidden = true) id: Long
    ): ProfileResponse
}
