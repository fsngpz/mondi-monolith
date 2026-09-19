package com.mondi.machine.accounts.profiles

import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

/**
 * The request model class for Profile with file upload support.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
data class ProfileFileRequest(
    val name: String? = null,
    val profilePicture: MultipartFile? = null,
    val profilePictureKey: String? = null,
    val mobile: String? = null,
    val membershipSince: OffsetDateTime? = null
)
