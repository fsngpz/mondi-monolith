package com.mondi.machine.accounts.profiles

import org.springframework.web.multipart.MultipartFile

/**
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
data class ProfileFileRequest(
    val name: String? = null,
    val address: String? = null,
//    @Parameter(
//        description = "The profile picture file to upload",
//        schema = Schema(type = "string", format = "binary")
//    )
    val profilePicture: MultipartFile? = null
)
