package com.mondi.machine.auths.refresh

import com.mondi.machine.auths.jwt.JwtService
import com.mondi.machine.configs.CustomUserDetails
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The REST controller for refresh token operations.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@RestController
@RequestMapping("/v1/auth/refresh")
@Tag(name = "Authentication API")
class RefreshTokenController(
    private val refreshTokenService: RefreshTokenService,
    private val jwtService: JwtService
) : RefreshTokenSwaggerController {

    /**
     * Refresh access token using refresh token.
     *
     * @param request the [RefreshTokenRequest] containing refresh token.
     * @return the [RefreshTokenResponse] with new access token and refresh token.
     */
    @PostMapping
    override fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<RefreshTokenResponse> {
        // -- validate refresh token --
        val refreshToken = refreshTokenService.validateRefreshToken(request.refreshToken)

        // -- get user from refresh token --
        val user = refreshToken.user

        // -- generate new access token --
        val customUserDetails = CustomUserDetails(user)
        val newBearerToken = jwtService.generateToken(customUserDetails)

        // -- rotate refresh token (revoke old, generate new) --
        val newRefreshToken = refreshTokenService.rotateRefreshToken(request.refreshToken, user)

        // -- return new tokens --
        val response = RefreshTokenResponse(
            bearerToken = newBearerToken,
            refreshToken = newRefreshToken
        )

        return ResponseEntity.ok(response)
    }
}
