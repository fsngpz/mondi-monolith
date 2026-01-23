package com.mondi.machine.accounts.profiles

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The rest controller for [Profile].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
@RestController
@RequestMapping("/v1/account")
class ProfileController(private val service: ProfileService) : ProfileSwaggerController {

    /**
     * a REST controller to handle request get the [ProfileResponse].
     *
     * @param httpServletRequest the [HttpServletRequest].
     * @return the [ProfileResponse]
     */
    @GetMapping("/profiles")
    override suspend fun get(
        @RequestAttribute("ID") id: Long
    ): ProfileResponse {
        // -- get the profile using ID --
        return service.get(id).toResponse()
    }

    /**
     * a REST controller to handle update the [Profile] instance.
     *
     * @param payload the [ProfileRequest] payload.
     * @param httpServletRequest the [HttpServletRequest].
     * @return the [ProfileResponse].
     */
    @PatchMapping("/profiles", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override suspend fun patch(
        @ModelAttribute payload: ProfileFileRequest,
        @RequestAttribute("ID") id: Long
    ): ProfileResponse {
        // -- create new instance ProfileRequest --
        val request = ProfileRequest(
            name = payload.name,
            mobile = payload.mobile,
            membershipSince = payload.membershipSince
        )
        // -- update the data Profile --
        return service.patch(id, request, payload.profilePicture).toResponse()
    }
}
