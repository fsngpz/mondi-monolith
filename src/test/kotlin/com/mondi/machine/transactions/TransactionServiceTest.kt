package com.mondi.machine.transactions

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.accounts.profiles.ProfileService
import com.mondi.machine.auths.users.User
import com.mondi.machine.storage.dropbox.DropboxService
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.multipart.MultipartFile

/**
 * The test class of [TransactionService].
 *
 * @author Ferdinand Sangap
 * @since 2024-05-08
 */
@SpringBootTest(classes = [TransactionService::class])
@ActiveProfiles("test")
internal class TransactionServiceTest(@Autowired private val transactionService: TransactionService) {
  // -- region of mock --
  @MockBean
  lateinit var mockProfileService: ProfileService

  @MockBean
  lateinit var mockDropboxService: DropboxService

  @MockBean
  lateinit var mockTransactionRepository: TransactionRepository
  // -- end of region mock --

  // -- region of smoke testing --
  @Test
  fun `dependencies are not null`() {
    assertThat(transactionService).isNotNull
    assertThat(mockProfileService).isNotNull
    assertThat(mockDropboxService).isNotNull
    assertThat(mockTransactionRepository).isNotNull
  }
  // -- end of region smoke testing --

  @Test
  fun `attempting to get but not found`() {
    // -- mock --
    whenever(mockTransactionRepository.findById(any<Long>())).thenReturn(Optional.empty())

    // -- execute --
    assertThrows<NoSuchElementException> { transactionService.get(1L) }

    // -- verify --
    verify(mockTransactionRepository).findById(any<Long>())
  }

  @Test
  fun `get and found`() {
    val mockUser = createMockProfile("myname@mail.com")
    val mockTransaction = Transaction(
      "TEST", BigDecimal("1000"),
      "this.url",
      OffsetDateTime.now(),
      mockUser
    )
    // -- mock --
    whenever(mockTransactionRepository.findById(any<Long>())).thenReturn(
      Optional.of(mockTransaction)
    )

    // -- execute --
    val result = transactionService.get(1L)
    assertThat(result).usingRecursiveComparison().isEqualTo(mockTransaction)

    // -- verify --
    verify(mockTransactionRepository).findById(any<Long>())
  }

  @Test
  fun `attempting to find all`() {
    val productsName = listOf("Stuff One", "Goods Two", "Things Three")
    val transactions = productsName.mapIndexed { index, value ->
      createMockTransaction(value, BigDecimal.ONE, createMockProfile("John $index"))
    }
    // -- mock --
    whenever(
      mockTransactionRepository.findAllCustom(
        any<Long>(),
        anyOrNull(),
        any<OffsetDateTime>(),
        any<OffsetDateTime>(),
        any<Pageable>()
      )
    ).thenReturn(PageImpl(transactions))

    // -- execute --
    val result = transactionService.findAll(
      1L,
      null,
      OffsetDateTime.now().minusYears(1000),
      OffsetDateTime.now(),
      Pageable.unpaged()
    )

    assertThat(result).isNotEmpty

    // -- verify --
    verify(mockTransactionRepository).findAllCustom(
      any<Long>(),
      anyOrNull(),
      any<OffsetDateTime>(),
      any<OffsetDateTime>(),
      any<Pageable>()
    )
  }

  @Test
  fun `attempting to create and success`() {
    val mockMultipartFile = MockMultipartFile("hello", ByteArray(1024))
    val mockProfile = createMockProfile("John Doe")
    val certificateUrl = "hello.url"
    val mockTransaction = createMockTransaction("ABC", BigDecimal.TEN, mockProfile)
    // -- mock --
    whenever(mockProfileService.get(any<Long>())).thenReturn(mockProfile)
    whenever(mockDropboxService.upload(any<Long>(), any<MultipartFile>())).thenReturn(
      certificateUrl
    )
    whenever(mockTransactionRepository.save(any<Transaction>())).thenReturn(mockTransaction)

    // -- execute --
    val result = transactionService.create(
      1L,
      "ABC",
      BigDecimal.TEN,
      mockMultipartFile,
      OffsetDateTime.now()
    )
    assertThat(result).usingRecursiveComparison().isEqualTo(mockTransaction)

    // -- verify --
    verify(mockProfileService).get(any<Long>())
    verify(mockDropboxService).upload(any<Long>(), any<MultipartFile>())
    verify(mockTransactionRepository).save(any<Transaction>())
  }

  @Test
  fun `attempting to update and success`() {
    val mockMultipartFile = MockMultipartFile("hello", ByteArray(1024))
    val mockProfile = createMockProfile("John Doe").apply { this.user.id = 1L }
    val certificateUrl = "hello.url"
    val mockRequest = TransactionRequest(
      "ABC",
      BigDecimal.TEN,
      mockMultipartFile,
      OffsetDateTime.now()
    )
    val mockTransaction = createMockTransaction("ABC", BigDecimal.TEN, mockProfile)
    // -- mock --
    whenever(mockTransactionRepository.findById(any<Long>())).thenReturn(
      Optional.of(mockTransaction)
    )
    whenever(mockDropboxService.upload(any<Long>(), any<MultipartFile>())).thenReturn(
      certificateUrl
    )
    whenever(mockTransactionRepository.save(any<Transaction>())).thenReturn(mockTransaction)

    // -- execute --
    val result = transactionService.update(1L, mockRequest)
    assertThat(result).usingRecursiveComparison().isEqualTo(mockTransaction)

    // -- verify --
    verify(mockTransactionRepository).findById(any<Long>())
    verify(mockDropboxService).upload(any<Long>(), any<MultipartFile>())
    verify(mockTransactionRepository).save(any<Transaction>())
  }

  @Test
  fun `attempting to update but user id is null`() {
    val mockMultipartFile = MockMultipartFile("hello", ByteArray(1024))
    val mockProfile = createMockProfile("John Doe")
    val mockRequest = TransactionRequest(
      "ABC",
      BigDecimal.TEN,
      mockMultipartFile,
      OffsetDateTime.now()
    )
    val mockTransaction = createMockTransaction("ABC", BigDecimal.TEN, mockProfile)
    // -- mock --
    whenever(mockTransactionRepository.findById(any<Long>())).thenReturn(
      Optional.of(mockTransaction)
    )
    // -- execute --
    assertThrows<IllegalArgumentException> { transactionService.update(1L, mockRequest) }

    // -- verify --
    verify(mockTransactionRepository).findById(any<Long>())
    verify(mockDropboxService, never()).upload(any<Long>(), any<MultipartFile>())
    verify(mockTransactionRepository, never()).save(any<Transaction>())
  }

  @Test
  fun `attempting to update but no transaction was found`() {
    val mockMultipartFile = MockMultipartFile("hello", ByteArray(1024))
    val mockRequest = TransactionRequest(
      "ABC",
      BigDecimal.TEN,
      mockMultipartFile,
      OffsetDateTime.now()
    )
    // -- mock --
    whenever(mockTransactionRepository.findById(any<Long>())).thenReturn(
      Optional.empty()
    )
    // -- execute --
    assertThrows<NoSuchElementException> { transactionService.update(1L, mockRequest) }

    // -- verify --
    verify(mockTransactionRepository).findById(any<Long>())
    verify(mockDropboxService, never()).upload(any<Long>(), any<MultipartFile>())
    verify(mockTransactionRepository, never()).save(any<Transaction>())
  }

  fun createMockTransaction(productName: String, price: BigDecimal, profile: Profile): Transaction {
    return Transaction(productName, price, "url.id", OffsetDateTime.now(), profile)
  }

  fun createMockProfile(name: String): Profile {
    val mockUser = User("email@hello.com", "password")
    return Profile(mockUser).apply {
      this.name = name
    }
  }
}