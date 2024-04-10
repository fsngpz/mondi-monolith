package com.mondi.machine.accounts.profiles

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
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
   * a REST controller to handle creating the new [Profile].
   *
   * @param request the [ProfileRequest] payload.
   * @param httpServletRequest the [HttpServletRequest].
   * @return the [ProfileResponse]
   */
  @PostMapping("/profiles")
  @ResponseStatus(HttpStatus.CREATED)
  fun create(
    @RequestBody request: ProfileRequest,
    httpServletRequest: HttpServletRequest
  ): ProfileResponse {
    // -- get the ID from header--
    val id = httpServletRequest.getHeader("ID").toLong()

    // -- validate the field name --
    requireNotNull(request.name) {
      "field 'name' cannot be null"
    }

    // -- create the new data Profile --
    return service.create(id, request.name, request.address, request.profilePicture).toResponse()
  }
}