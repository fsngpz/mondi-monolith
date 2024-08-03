package com.mondi.machine.backoffices.transactions

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.auths.users.User
import com.mondi.machine.transactions.Transaction
import com.mondi.machine.transactions.TransactionRequest
import com.mondi.machine.transactions.TransactionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * The test class for [BackofficeTransactionService].
 *
 * @author Ferdinand Sangap.
 * @since 2024-08-03
 */
@SpringBootTest(classes = [BackofficeTransactionService::class])
@ActiveProfiles("test")
internal class BackofficeTransactionServiceTest(
    @Autowired private val backofficeTransactionService: BackofficeTransactionService
) {
    @MockBean
    private lateinit var mockTransactionService: TransactionService

    @Test
    fun `dependencies are not null`() {
        assertThat(backofficeTransactionService).isNotNull
        assertThat(mockTransactionService).isNotNull
    }

    @Test
    fun `create transaction success`() {
        val mockMultipartFile = MockMultipartFile("hello", ByteArray(1024))
        val mockRequest = BackofficeTransactionRequest(
            userId = 1L,
            productName = "TEST",
            price = BigDecimal(1.0),
            certificateFile = mockMultipartFile,
            purchasedAt = OffsetDateTime.now()
        )
        val mockTransaction = createMockTransaction("product one")
        // -- mock --
        whenever(
            mockTransactionService.create(
                any<Long>(),
                any<String>(),
                any<BigDecimal>(),
                any<MultipartFile>(),
                any<OffsetDateTime>()
            )
        ).thenReturn(mockTransaction)

        // -- execute --
        val result = backofficeTransactionService.create(mockRequest)
        assertThat(result.productName).isEqualTo(mockTransaction.productName)
        assertThat(result.price).isEqualTo(mockTransaction.price)
        assertThat(result.certificateUrl).isEqualTo(mockTransaction.certificateUrl)

        // -- verify --
        verify(mockTransactionService).create(
            any<Long>(),
            any<String>(),
            any<BigDecimal>(),
            any<MultipartFile>(),
            any<OffsetDateTime>()
        )
    }

    @Test
    fun `update transaction success`() {
        val mockMultipartFile = MockMultipartFile("hello", ByteArray(1024))
        val mockRequest = BackofficeTransactionRequest(
            userId = 1L,
            productName = "TEST",
            price = BigDecimal(1.0),
            certificateFile = mockMultipartFile,
            purchasedAt = OffsetDateTime.now()
        )
        val mockTransaction = createMockTransaction("product one")
        // -- mock --
        whenever(
            mockTransactionService.update(
                any<Long>(),
                any<TransactionRequest>()
            )
        ).thenReturn(mockTransaction)

        // -- execute --
        val result = backofficeTransactionService.update(1L, mockRequest)
        assertThat(result.productName).isEqualTo(mockTransaction.productName)
        assertThat(result.price).isEqualTo(mockTransaction.price)
        assertThat(result.certificateUrl).isEqualTo(mockTransaction.certificateUrl)

        // -- verify --
        verify(mockTransactionService).update(
            any<Long>(),
            any<TransactionRequest>()
        )
    }

    private fun createMockTransaction(productName: String): Transaction {
        return Transaction(
            productName = productName,
            price = BigDecimal.TEN,
            certificateUrl = "url",
            purchasedAt = OffsetDateTime.now(),
            profile = Profile(User("mail", "pass"))
        ).apply {
            this.id = 1
        }
    }
}
