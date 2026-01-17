package com.mondi.machine.accounts.profiles

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
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
class ProfileController(private val service: ProfileService) {

    /**
     * a REST controller to handle request get the [ProfileResponse].
     *
     * @param httpServletRequest the [HttpServletRequest].
     * @return the [ProfileResponse]
     */
    @GetMapping("/profiles")
    fun get(httpServletRequest: HttpServletRequest): ProfileResponse {
        // -- get the ID from header--
        val id = httpServletRequest.getHeader("ID").toLong()
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
    @PatchMapping("/profiles")
    suspend fun patch(
        @ModelAttribute payload: ProfileFileRequest,
        httpServletRequest: HttpServletRequest
    ): ProfileResponse {
        // -- get the ID from header--
        val id = httpServletRequest.getHeader("ID").toLong()
        // -- create new instance ProfileRequest --
        val request = ProfileRequest(name = payload.name, payload.address)

        // -- update the data Profile --
        return service.patch(id, request, payload.profilePicture).toResponse()
    }
}
