package com.mondi.machine.orders

import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * The test class for [CertificateController].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
internal class CertificateControllerTest(@Autowired private val mockMvc: MockMvc) {

    @MockitoBean
    private lateinit var mockCertificateService: CertificateService

    @Test
    fun `get my certificates returns the list for the authenticated user`() {
        whenever(mockCertificateService.findAllByUser(7L)).thenReturn(
            listOf(
                CertificateResponse(
                    source = CertificateSource.ORDER,
                    orderItemId = 100L,
                    orderId = 10L,
                    productName = "Solitaire Ring",
                    productSku = "RING_26_001",
                    certificateUrl = "certificates/7/order-10-item-100.pdf",
                    mediaUrl = null,
                    purchasedAt = OffsetDateTime.parse("2026-11-01T10:00:00Z")
                )
            )
        )

        // -- execute --
        mockMvc.perform(get("/v1/account/certificates").requestAttr("ID", 7L))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].source").value("ORDER"))
            .andExpect(jsonPath("$[0].orderId").value(10))
            .andExpect(jsonPath("$[0].productName").value("Solitaire Ring"))
            .andExpect(jsonPath("$[0].certificateUrl").value("certificates/7/order-10-item-100.pdf"))

        // -- verify --
        verify(mockCertificateService).findAllByUser(7L)
    }

    @Test
    fun `get my certificates without any returns an empty list`() {
        whenever(mockCertificateService.findAllByUser(7L)).thenReturn(emptyList())

        // -- execute --
        mockMvc.perform(get("/v1/account/certificates").requestAttr("ID", 7L))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }
}
