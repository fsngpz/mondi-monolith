package com.mondi.machine

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * The interface for Root Swagger Controller.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-22
 */
@Tag(name = "Root API")
interface RootSwaggerController {

    @Operation(summary = "Health check")
    fun root(): String

    @Operation(summary = "Echo request body")
    fun post(data: Any): Any
}
