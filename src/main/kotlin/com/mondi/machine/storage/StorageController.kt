package com.mondi.machine.storage

import com.mondi.machine.storage.supabase.SupabaseService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.time.Duration.Companion.minutes

/**
 * The REST controller class for storage.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-17
 */
@RestController
@RequestMapping("/v1/storage")
class StorageController(private val service: SupabaseService) {

    /**
     * a GET request to handle get signed URL for file download.
     *
     * @param fileKey the file key or path.
     * @return the [FileSignedUrlResponse].
     */
    @GetMapping("/file/signed-url")
    suspend fun getSignedUrl(
        @RequestParam fileKey: String,
        @RequestParam expireDurationInMinutes: Int? = null
    ): FileSignedUrlResponse {
        return service.getSignedUrl(fileKey, expireDurationInMinutes?.minutes ?: 15.minutes)
    }
}
