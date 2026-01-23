package com.mondi.machine.accounts.profiles

import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

/**
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
data class ProfileFileRequest(
    val name: String? = null,
    val profilePicture: MultipartFile? = null,
    val mobile: String? = null,
    val membershipSince: OffsetDateTime? = null
)
