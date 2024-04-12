package com.mondi.machine.accounts.profiles

import com.fasterxml.jackson.databind.ObjectMapper
import com.mondi.machine.auths.users.User
import com.mondi.machine.storage.dropbox.DropboxService
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile

/**
 * The test class for [ProfileService].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
@SpringBootTest(classes = [ProfileService::class])
@Import(value = [ObjectMapper::class])
internal class ProfileServiceTest(@Autowired private val service: ProfileService) {
  // -- region of mock --
  @MockBean
  lateinit var mockRepository: ProfileRepository

  @MockBean
  lateinit var mockDropboxService: DropboxService
  // -- end of region mock --

  // -- region of smoke testing --
  @Test
  fun `dependencies are not null`() {
    assertThat(service).isNotNull
    assertThat(mockRepository).isNotNull
    assertThat(mockDropboxService).isNotNull
  }
  // -- end of region smoke testing --

  @Test
  fun `get instance but not found`() {
    // -- mock --
    whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.empty())

    // -- execute --
    assertThrows<NoSuchElementException> { service.get(0L) }

    // -- verify --
    verify(mockRepository).findById(any<Long>())
  }

  @Test
  fun `get and found`() {
    val mockProfile = Profile(user = User("mail", "pw"))
    // -- mock --
    whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))

    // -- execute --
    val result = service.get(0L)

    // -- verify --
    assertThat(result).usingRecursiveComparison().isEqualTo(mockProfile)

    verify(mockRepository).findById(any<Long>())
  }

  @Test
  fun `create new profile`() {
    val mockUser = User("mail", "pw")
    val mockProfile = Profile(user = mockUser)

    // -- mock --
    whenever(mockRepository.save(any<Profile>())).thenReturn(mockProfile)

    // -- execute --
    val result = service.create(mockUser)

    // -- verify --
    assertThat(result).usingRecursiveComparison().isEqualTo(mockProfile)

    // -- verify --
    verify(mockRepository).save(any<Profile>())
  }

  @Test
  fun `update but name is null`() {
    val mockRequest = ProfileRequest(address = "address in here")
    // -- execute --
    assertThrows<IllegalArgumentException> { service.update(0L, mockRequest) }

    // -- verify --
    verify(mockRepository, never()).findById(any<Long>())
    verify(mockRepository, never()).save(any<Profile>())
  }

  @Test
  fun `update but no profile was found`() {
    val mockRequest = ProfileRequest(name = "Lorem", address = "address in here")
    // -- mock --
    whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.empty())

    // -- execute --
    assertThrows<NoSuchElementException> { service.update(1L, mockRequest) }

    // -- verify --
    verify(mockRepository, never()).save(any<Profile>())
  }

  @Test
  fun `update and success`() {
    val mockUser = User("mail", "pw")
    val mockProfile = Profile(user = mockUser)
    val mockRequest = ProfileRequest(name = "Lorem", address = "address in here")
    // -- mock --
    whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
    whenever(mockRepository.save(any<Profile>())).thenReturn(mockProfile)
    // -- execute --
    val result = service.update(1L, mockRequest)

    // -- verify --
    assertThat(result).usingRecursiveComparison().isEqualTo(mockProfile)
    verify(mockRepository).save(any<Profile>())
  }

  @Test
  fun `patch but no profile was found`() {
    val mockRequest = ProfileRequest(name = "Lorem", address = "address in here")
    val mockProfilePicture = MockMultipartFile("hello", ByteArray(0))

    // -- mock --
    whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.empty())

    // -- execute --
    assertThrows<NoSuchElementException> { service.patch(0L, mockRequest, mockProfilePicture) }

    // -- verify --
    verify(mockRepository, never()).save(any<Profile>())
  }

  @Test
  fun `patch WITH profile picture and save`() {
    val mockUser = User("mail", "pw")
    val mockProfile = Profile(user = mockUser)

    val mockRequest = ProfileRequest(name = "Lorem", address = "address in here")
    val mockProfilePicture = MockMultipartFile("hello", ByteArray(0))
    val urlProfilePicture = "this.is.url"
    // -- mock --
    whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
    whenever(mockDropboxService.upload(any<Long>(), any<MultipartFile>())).thenReturn(
      urlProfilePicture
    )
    whenever(mockRepository.save(any<Profile>())).thenReturn(mockProfile)
    // -- execute --
    val result = service.patch(0L, mockRequest, mockProfilePicture)

    // -- capture --
    val profileCaptor = argumentCaptor<Profile>()

    // -- verify --
    assertThat(result).usingRecursiveComparison().isEqualTo(mockProfile)

    verify(mockRepository).save(any<Profile>())
    verify(mockDropboxService).upload(any<Long>(), any<MultipartFile>())
    // -- verify the captor --
    verify(mockRepository).save(profileCaptor.capture())
    val profileCaptured = profileCaptor.firstValue
    assertThat(profileCaptured.profilePictureUrl).isEqualTo(urlProfilePicture)
  }

  @Test
  fun `patch WITHOUT profile picture and save`() {
    val mockUser = User("mail", "pw")
    val mockProfile = Profile(user = mockUser)

    val mockRequest = ProfileRequest(name = "Lorem", address = "address in here")
    val mockProfilePicture = null
    // -- mock --
    whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
    whenever(mockRepository.save(any<Profile>())).thenReturn(mockProfile)
    // -- execute --
    val result = service.patch(0L, mockRequest, mockProfilePicture)

    // -- capture --
    val profileCaptor = argumentCaptor<Profile>()

    // -- verify --
    assertThat(result).usingRecursiveComparison().isEqualTo(mockProfile)

    verify(mockRepository).save(any<Profile>())
    verify(mockDropboxService, never()).upload(any<Long>(), any<MultipartFile>())
    // -- verify the captor --
    verify(mockRepository).save(profileCaptor.capture())
    val profileCaptured = profileCaptor.firstValue
    assertThat(profileCaptured.profilePictureUrl).isNull()
  }
}