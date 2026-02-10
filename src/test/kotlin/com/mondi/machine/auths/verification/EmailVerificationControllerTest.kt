package com.mondi.machine.auths.verification

import com.mondi.machine.auths.users.User
import com.mondi.machine.exceptions.EmailAlreadyVerifiedException
import com.mondi.machine.exceptions.EmailVerificationTokenNotFoundException
import com.mondi.machine.exceptions.InvalidEmailVerificationTokenException
import com.mondi.machine.exceptions.TooManyRequestsException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * The test class of [EmailVerificationController].
 *
 * @author Ferdinand Sangap
 * @since 2026-02-09
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
internal class EmailVerificationControllerTest(@Autowired private val mockMvc: MockMvc) {

    @MockitoBean
    private lateinit var mockVerificationTokenService: EmailVerificationTokenService

    @MockitoBean
    private lateinit var mockResendVerificationService: ResendVerificationService

    @Test
    fun `verifyEmail GET endpoint successfully verifies valid token`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }
        whenever(mockVerificationTokenService.verifyEmail("valid-token")).thenReturn(mockUser)

        // -- act & assert --
        mockMvc.perform(
            get("/v1/auth/verify-email")
                .param("token", "valid-token")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.verified").value(true))

        verify(mockVerificationTokenService).verifyEmail("valid-token")
    }

    @Test
    fun `verifyEmail GET endpoint returns 404 when token not found`() {
        // -- arrange --
        whenever(mockVerificationTokenService.verifyEmail("non-existent"))
            .thenThrow(EmailVerificationTokenNotFoundException("Verification token not found"))

        // -- act & assert --
        mockMvc.perform(
            get("/v1/auth/verify-email")
                .param("token", "non-existent")
        )
            .andExpect(status().isNotFound)

        verify(mockVerificationTokenService).verifyEmail("non-existent")
    }

    @Test
    fun `verifyEmail GET endpoint returns 400 when token is expired`() {
        // -- arrange --
        whenever(mockVerificationTokenService.verifyEmail("expired-token"))
            .thenThrow(InvalidEmailVerificationTokenException("Verification token has expired"))

        // -- act & assert --
        mockMvc.perform(
            get("/v1/auth/verify-email")
                .param("token", "expired-token")
        )
            .andExpect(status().isBadRequest)

        verify(mockVerificationTokenService).verifyEmail("expired-token")
    }

    @Test
    fun `verifyEmail GET endpoint returns 400 when token already used`() {
        // -- arrange --
        whenever(mockVerificationTokenService.verifyEmail("used-token"))
            .thenThrow(InvalidEmailVerificationTokenException("This verification link has already been used"))

        // -- act & assert --
        mockMvc.perform(
            get("/v1/auth/verify-email")
                .param("token", "used-token")
        )
            .andExpect(status().isBadRequest)

        verify(mockVerificationTokenService).verifyEmail("used-token")
    }

    @Test
    fun `verifyEmailPost POST endpoint successfully verifies valid token`() {
        // -- arrange --
        val mockUser = User("test@example.com", "password").apply { this.id = 1L }
        whenever(mockVerificationTokenService.verifyEmail("valid-token")).thenReturn(mockUser)

        val requestBody = """{"token": "valid-token"}"""

        // -- act & assert --
        mockMvc.perform(
            post("/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.verified").value(true))

        verify(mockVerificationTokenService).verifyEmail("valid-token")
    }

    @Test
    fun `verifyEmailPost POST endpoint returns 404 when token not found`() {
        // -- arrange --
        whenever(mockVerificationTokenService.verifyEmail("non-existent"))
            .thenThrow(EmailVerificationTokenNotFoundException("Verification token not found"))

        val requestBody = """{"token": "non-existent"}"""

        // -- act & assert --
        mockMvc.perform(
            post("/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)

        verify(mockVerificationTokenService).verifyEmail("non-existent")
    }

    @Test
    fun `verifyEmailPost POST endpoint returns 400 when token is expired`() {
        // -- arrange --
        whenever(mockVerificationTokenService.verifyEmail("expired-token"))
            .thenThrow(InvalidEmailVerificationTokenException("Verification token has expired"))

        val requestBody = """{"token": "expired-token"}"""

        // -- act & assert --
        mockMvc.perform(
            post("/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)

        verify(mockVerificationTokenService).verifyEmail("expired-token")
    }

    @Test
    fun `verifyEmail GET endpoint validates message content`() {
        // -- arrange --
        val mockUser = User("verified@example.com", "password").apply { this.id = 1L }
        whenever(mockVerificationTokenService.verifyEmail("valid-token")).thenReturn(mockUser)

        // -- act & assert --
        mockMvc.perform(
            get("/v1/auth/verify-email")
                .param("token", "valid-token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Email verified successfully! You can now log in to your account."))
            .andExpect(jsonPath("$.email").value("verified@example.com"))
            .andExpect(jsonPath("$.verified").value(true))
    }

    @Test
    fun `resendVerification POST endpoint successfully resends verification email`() {
        // -- arrange --
        val response = ResendVerificationResponse(
            message = "Verification email has been sent. Please check your inbox.",
            email = "test@example.com",
            expiresInHours = 24
        )
        whenever(mockResendVerificationService.resendVerification("test@example.com")).thenReturn(response)

        val requestBody = """{"email": "test@example.com"}"""

        // -- act & assert --
        mockMvc.perform(
            post("/v1/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Verification email has been sent. Please check your inbox."))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.expiresInHours").value(24))

        verify(mockResendVerificationService).resendVerification("test@example.com")
    }

    @Test
    fun `resendVerification POST endpoint returns 404 when user not found`() {
        // -- arrange --
        whenever(mockResendVerificationService.resendVerification("notfound@example.com"))
            .thenThrow(EmailVerificationTokenNotFoundException("User with email 'notfound@example.com' not found"))

        val requestBody = """{"email": "notfound@example.com"}"""

        // -- act & assert --
        mockMvc.perform(
            post("/v1/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)

        verify(mockResendVerificationService).resendVerification("notfound@example.com")
    }

    @Test
    fun `resendVerification POST endpoint returns 409 when email already verified`() {
        // -- arrange --
        whenever(mockResendVerificationService.resendVerification("verified@example.com"))
            .thenThrow(EmailAlreadyVerifiedException("Email address is already verified"))

        val requestBody = """{"email": "verified@example.com"}"""

        // -- act & assert --
        mockMvc.perform(
            post("/v1/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isConflict)

        verify(mockResendVerificationService).resendVerification("verified@example.com")
    }

    @Test
    fun `resendVerification POST endpoint returns 429 when rate limit exceeded`() {
        // -- arrange --
        whenever(mockResendVerificationService.resendVerification("ratelimited@example.com"))
            .thenThrow(TooManyRequestsException("Too many verification requests. Please try again in 60 minute(s)."))

        val requestBody = """{"email": "ratelimited@example.com"}"""

        // -- act & assert --
        mockMvc.perform(
            post("/v1/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isTooManyRequests)

        verify(mockResendVerificationService).resendVerification("ratelimited@example.com")
    }
}
