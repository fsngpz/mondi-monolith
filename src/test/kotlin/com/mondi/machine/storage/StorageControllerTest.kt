package com.mondi.machine.storage

import com.mondi.machine.storage.supabase.SupabaseService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * The test class for [StorageController].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
internal class StorageControllerTest(@Autowired private val mockMvc: MockMvc) {

    @MockitoBean
    private lateinit var mockSupabaseService: SupabaseService

    @MockitoBean
    private lateinit var mockAccessService: StorageAccessService

    private val key = "certificates/7/order-10-item-100.pdf"

    private fun user(vararg roles: String) =
        UsernamePasswordAuthenticationToken("user", null, roles.map { SimpleGrantedAuthority(it) })

    private fun signedUrl(userId: Long, vararg roles: String) =
        get("/v1/storage/file/signed-url").param("fileKey", key)
            .requestAttr("ID", userId).principal(user(*roles))

    @Test
    fun `a customer is checked as a customer`() {
        val started = mockMvc.perform(signedUrl(7L, "ROLE_USER")).andReturn()
        mockMvc.perform(asyncDispatch(started)).andExpect(status().isOk)

        // -- verify --
        verify(mockAccessService).requireAccess(7L, false, key)
    }

    @Test
    fun `an admin is checked as an admin`() {
        val started = mockMvc.perform(signedUrl(1L, "ROLE_ADMIN")).andReturn()
        mockMvc.perform(asyncDispatch(started)).andExpect(status().isOk)

        // -- verify --
        verify(mockAccessService).requireAccess(1L, true, key)
    }

    @Test
    fun `another customer's file returns 404 and no URL is created`() {
        doThrow(NoSuchElementException("no file was found with key '$key'"))
            .whenever(mockAccessService).requireAccess(8L, false, key)

        // -- execute --
        val started = mockMvc.perform(signedUrl(8L, "ROLE_USER")).andReturn()
        mockMvc.perform(asyncDispatch(started)).andExpect(status().isNotFound)

        // -- verify --
        verifyNoInteractions(mockSupabaseService)
    }

    @ParameterizedTest
    @CsvSource(", 15", "30, 30", "60, 60", "10080, 60", "0, 1", "-5, 1")
    fun `signed URL expiry is kept between 1 and 60 minutes`(requested: Int?, expected: Int) {
        assertThat(StorageController.expiryMinutes(requested)).isEqualTo(expected)
    }
}
