package com.mondi.machine.storage

import com.mondi.machine.accounts.profiles.ProfileRepository
import com.mondi.machine.orders.OrderItemRepository
import com.mondi.machine.orders.OrderRepository
import com.mondi.machine.transactions.TransactionRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * The test class for [StorageAccessService].
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@SpringBootTest(classes = [StorageAccessService::class])
internal class StorageAccessServiceTest(@Autowired private val service: StorageAccessService) {

    @MockitoBean
    lateinit var mockProfileRepository: ProfileRepository

    @MockitoBean
    lateinit var mockOrderRepository: OrderRepository

    @MockitoBean
    lateinit var mockOrderItemRepository: OrderItemRepository

    @MockitoBean
    lateinit var mockTransactionRepository: TransactionRepository

    private val key = "payment-proofs/7/order-10-1762500000.png"

    @Test
    fun `admin can open any file without an ownership lookup`() {
        assertDoesNotThrow { service.requireAccess(1L, isAdmin = true, fileKey = key) }

        verify(mockOrderRepository, never()).existsByProfileIdAndPaymentProofUrl(any(), any())
    }

    @Test
    fun `owner of a payment proof can open it`() {
        whenever(mockOrderRepository.existsByProfileIdAndPaymentProofUrl(7L, key)).thenReturn(true)

        assertDoesNotThrow { service.requireAccess(7L, isAdmin = false, fileKey = key) }
    }

    @Test
    fun `owner of an order item certificate can open it`() {
        val certificate = "certificates/7/order-10-item-100.pdf"
        whenever(mockOrderItemRepository.existsByOrder_Profile_IdAndCertificateUrl(7L, certificate)).thenReturn(true)

        assertDoesNotThrow { service.requireAccess(7L, isAdmin = false, fileKey = certificate) }
    }

    @Test
    fun `owner of a legacy certificate can open it`() {
        val certificate = "certificates/7/legacy.pdf"
        whenever(mockTransactionRepository.existsByProfileIdAndCertificateUrl(7L, certificate)).thenReturn(true)

        assertDoesNotThrow { service.requireAccess(7L, isAdmin = false, fileKey = certificate) }
    }

    @Test
    fun `owner of a profile picture can open it`() {
        val picture = "users/profile-picture/user-7.jpg"
        whenever(mockProfileRepository.existsByIdAndProfilePictureUrl(7L, picture)).thenReturn(true)

        assertDoesNotThrow { service.requireAccess(7L, isAdmin = false, fileKey = picture) }
    }

    @Test
    fun `another customer's file is reported as not found`() {
        // -- every ownership lookup returns false for user 8 (mock default) --
        assertThrows<NoSuchElementException> { service.requireAccess(8L, isAdmin = false, fileKey = key) }
    }
}
