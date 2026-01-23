package com.mondi.machine.auths.refresh

import com.mondi.machine.auths.users.OAuthProvider
import com.mondi.machine.auths.users.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.ZonedDateTime

/**
 * The unit test class for [RefreshTokenService].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
class RefreshTokenServiceTest {

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @InjectMocks
    private lateinit var refreshTokenService: RefreshTokenService

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testUser = User(
            email = "test@example.com",
            password = "password123",
            provider = OAuthProvider.LOCAL
        )
    }

    @Test
    fun `test dependencies are not null`() {
        // -- assert --
        assertThat(refreshTokenRepository).isNotNull
        assertThat(refreshTokenService).isNotNull
    }

    @Test
    fun `test generate refresh token successfully`() {
        // -- arrange --
        val mockRefreshToken = RefreshToken(
            token = "test-token",
            user = testUser,
            expiresAt = ZonedDateTime.now().plusDays(7)
        )
        `when`(refreshTokenRepository.save(org.mockito.kotlin.any())).thenReturn(mockRefreshToken)

        // -- act --
        val token = refreshTokenService.generateRefreshToken(testUser)

        // -- assert --
        assertThat(token).isNotNull
        assertThat(token).isNotEmpty()
        verify(refreshTokenRepository).save(org.mockito.kotlin.any())
    }

    @Test
    fun `test validate refresh token successfully`() {
        // -- arrange --
        val tokenString = "valid-token"
        val refreshToken = RefreshToken(
            token = tokenString,
            user = testUser,
            expiresAt = ZonedDateTime.now().plusDays(7)
        )
        `when`(refreshTokenRepository.findByToken(tokenString)).thenReturn(refreshToken)

        // -- act --
        val result = refreshTokenService.validateRefreshToken(tokenString)

        // -- assert --
        assertThat(result).isNotNull
        assertThat(result.token).isEqualTo(tokenString)
        assertThat(result.user).isEqualTo(testUser)
    }

    @Test
    fun `test validate refresh token throws exception when token not found`() {
        // -- arrange --
        val tokenString = "invalid-token"
        `when`(refreshTokenRepository.findByToken(tokenString)).thenReturn(null)

        // -- act & assert --
        val exception = assertThrows(IllegalArgumentException::class.java) {
            refreshTokenService.validateRefreshToken(tokenString)
        }
        assertThat(exception.message).isEqualTo("Invalid refresh token")
    }

    @Test
    fun `test validate refresh token throws exception when token is expired`() {
        // -- arrange --
        val tokenString = "expired-token"
        val expiredToken = RefreshToken(
            token = tokenString,
            user = testUser,
            expiresAt = ZonedDateTime.now().minusDays(1) // expired yesterday
        )
        `when`(refreshTokenRepository.findByToken(tokenString)).thenReturn(expiredToken)

        // -- act & assert --
        val exception = assertThrows(IllegalArgumentException::class.java) {
            refreshTokenService.validateRefreshToken(tokenString)
        }
        assertThat(exception.message).isEqualTo("Refresh token has expired")
    }

    @Test
    fun `test validate refresh token throws exception when token is revoked`() {
        // -- arrange --
        val tokenString = "revoked-token"
        val revokedToken = RefreshToken(
            token = tokenString,
            user = testUser,
            expiresAt = ZonedDateTime.now().plusDays(7)
        )
        revokedToken.revoke()
        `when`(refreshTokenRepository.findByToken(tokenString)).thenReturn(revokedToken)

        // -- act & assert --
        val exception = assertThrows(IllegalArgumentException::class.java) {
            refreshTokenService.validateRefreshToken(tokenString)
        }
        assertThat(exception.message).isEqualTo("Refresh token has been revoked")
    }

    @Test
    fun `test revoke refresh token successfully`() {
        // -- arrange --
        val tokenString = "token-to-revoke"
        val refreshToken = RefreshToken(
            token = tokenString,
            user = testUser,
            expiresAt = ZonedDateTime.now().plusDays(7)
        )
        `when`(refreshTokenRepository.findByToken(tokenString)).thenReturn(refreshToken)
        `when`(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken)

        // -- act --
        refreshTokenService.revokeRefreshToken(tokenString)

        // -- assert --
        verify(refreshTokenRepository).findByToken(tokenString)
        verify(refreshTokenRepository).save(refreshToken)
        assertThat(refreshToken.isRevoked()).isTrue
    }

    @Test
    fun `test revoke all user tokens successfully`() {
        // -- arrange --
        val token1 = RefreshToken("token1", testUser, ZonedDateTime.now().plusDays(7))
        val token2 = RefreshToken("token2", testUser, ZonedDateTime.now().plusDays(7))
        val tokens = listOf(token1, token2)
        `when`(refreshTokenRepository.findAllByUser(testUser)).thenReturn(tokens)
        `when`(refreshTokenRepository.saveAll(tokens)).thenReturn(tokens)

        // -- act --
        refreshTokenService.revokeAllUserTokens(testUser)

        // -- assert --
        verify(refreshTokenRepository).findAllByUser(testUser)
        verify(refreshTokenRepository).saveAll(tokens)
        assertThat(token1.isRevoked()).isTrue
        assertThat(token2.isRevoked()).isTrue
    }

    @Test
    fun `test rotate refresh token successfully`() {
        // -- arrange --
        val oldTokenString = "old-token"
        val oldRefreshToken = RefreshToken(
            token = oldTokenString,
            user = testUser,
            expiresAt = ZonedDateTime.now().plusDays(7)
        )
        val newRefreshToken = RefreshToken(
            token = "new-token",
            user = testUser,
            expiresAt = ZonedDateTime.now().plusDays(7)
        )
        `when`(refreshTokenRepository.findByToken(oldTokenString)).thenReturn(oldRefreshToken)
        `when`(refreshTokenRepository.save(org.mockito.kotlin.any())).thenReturn(newRefreshToken)

        // -- act --
        val newToken = refreshTokenService.rotateRefreshToken(oldTokenString, testUser)

        // -- assert --
        assertThat(newToken).isNotNull
        assertThat(newToken).isNotEmpty()
        assertThat(oldRefreshToken.isRevoked()).isTrue
    }
}
