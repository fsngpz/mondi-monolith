package com.mondi.machine.storage

import com.mondi.machine.configs.WebSecurityConfig.Companion.ROLE_ADMIN
import com.mondi.machine.storage.supabase.SupabaseService
import kotlin.time.Duration.Companion.minutes
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller class for storage.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-17
 */
@RestController
@RequestMapping("/v1/storage")
class StorageController(
    private val service: SupabaseService,
    private val accessService: StorageAccessService
) : StorageSwaggerController {

    /**
     * a GET request to handle get signed URL for file download.
     * Customers only get URLs for their own files; admins for any file.
     *
     * @param fileKey the file key or path.
     * @param expireDurationInMinutes how long the URL stays valid, 1 to [MAX_EXPIRE_MINUTES] (default 15).
     * @param userId the user ID from request attribute.
     * @param authentication the signed-in user.
     * @return the [FileSignedUrlResponse].
     */
    @GetMapping("/file/signed-url")
    override suspend fun getSignedUrl(
        @RequestParam fileKey: String,
        @RequestParam expireDurationInMinutes: Int?,
        @RequestAttribute("ID") userId: Long,
        authentication: Authentication?
    ): FileSignedUrlResponse {
        // -- only the owner of the file, or an admin --
        val isAdmin = authentication?.authorities?.any { it.authority == "ROLE_$ROLE_ADMIN" } == true
        accessService.requireAccess(userId, isAdmin, fileKey)
        // -- keep links short-lived --
        return service.getSignedUrl(fileKey, expiryMinutes(expireDurationInMinutes).minutes)
    }

    companion object {
        const val DEFAULT_EXPIRE_MINUTES = 15
        const val MAX_EXPIRE_MINUTES = 60

        /**
         * the signed URL lifetime to use: the requested minutes kept within 1..[MAX_EXPIRE_MINUTES].
         *
         * @param requested the requested minutes, or null for [DEFAULT_EXPIRE_MINUTES].
         * @return the minutes to use.
         */
        fun expiryMinutes(requested: Int?): Int =
            (requested ?: DEFAULT_EXPIRE_MINUTES).coerceIn(1, MAX_EXPIRE_MINUTES)
    }
}
