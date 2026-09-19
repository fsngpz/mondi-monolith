package com.mondi.machine.accounts.profiles

import com.fasterxml.jackson.databind.ObjectMapper
import com.mondi.machine.auths.users.OAuthProvider
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRepository
import com.mondi.machine.exceptions.MobileAlreadyExistsException
import com.mondi.machine.storage.supabase.SupabaseService
import kotlinx.coroutines.test.runTest
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
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.multipart.MultipartFile
import java.util.Optional

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
    @MockitoBean
    lateinit var mockRepository: ProfileRepository

    @MockitoBean
    lateinit var mockSupabaseService: SupabaseService

    @MockitoBean
    lateinit var mockUserRepository: UserRepository
    // -- end of region mock --

    // -- region of smoke testing --
    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
        assertThat(mockRepository).isNotNull
        assertThat(mockSupabaseService).isNotNull
        assertThat(mockUserRepository).isNotNull
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
        val result = service.create(mockUser, "http://profile.picture.url/image.png")

        // -- verify --
        assertThat(result).usingRecursiveComparison().isEqualTo(mockProfile)

        // -- verify --
        verify(mockRepository).save(any<Profile>())
    }

    @Test
    fun `update but name is null`() {
        val mockRequest = ProfileRequest()
        // -- execute --
        assertThrows<IllegalArgumentException> { service.update(0L, mockRequest) }

        // -- verify --
        verify(mockRepository, never()).findById(any<Long>())
        verify(mockRepository, never()).save(any<Profile>())
    }

    @Test
    fun `update but no profile was found`() {
        val mockRequest = ProfileRequest(name = "Lorem")
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
        val mockRequest = ProfileRequest(name = "Lorem")
        // -- mock --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
        whenever(mockRepository.save(any<Profile>())).thenReturn(mockProfile)
        // -- execute --
        val result = service.update(1L, mockRequest)

        // -- verify --
        assertThat(result).usingRecursiveComparison().isEqualTo(mockProfile)
        verify(mockRepository).save(any<Profile>())
        // User save is handled by cascade, no direct call needed
        verify(mockUserRepository, never()).save(any<User>())
    }

    @Test
    fun `patch but no profile was found`() = runTest {
        val mockRequest = ProfileRequest(name = "Lorem")
        val mockProfilePicture = MockMultipartFile("hello", ByteArray(0))

        // -- mock --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { service.patch(0L, mockRequest, mockProfilePicture) }

        // -- verify --
        verify(mockRepository, never()).save(any<Profile>())
    }

    @Test
    fun `patch WITH profile picture and save`() = runTest {
        val mockUser = User("mail", "pw")
        val mockProfile = Profile(user = mockUser)

        val mockRequest = ProfileRequest(name = "Lorem")
        val mockProfilePicture = MockMultipartFile("hello", ByteArray(0))
        val urlProfilePicture = "this.is.url"
        // -- mock --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
        whenever(
            mockSupabaseService.uploadFile(
                any<String>(),
                any<String>(),
                any<MultipartFile>(),
                any<Boolean>()
            )
        ).thenReturn(
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
        verify(mockSupabaseService).uploadFile(any<String>(), any<String>(), any<MultipartFile>(), any<Boolean>())
        // -- verify the captor --
        verify(mockRepository).save(profileCaptor.capture())
        val profileCaptured = profileCaptor.firstValue
        assertThat(profileCaptured.profilePictureUrl).isEqualTo(urlProfilePicture)
    }

    @Test
    fun `patch WITHOUT profile picture and WITHOUT profilePictureKey preserves existing`() = runTest {
        val mockUser = User("mail", "pw")
        val existingUrl = "existing-url.jpg"
        val mockProfile = Profile(user = mockUser).apply {
            profilePictureUrl = existingUrl
        }

        val mockRequest = ProfileRequest(name = "Lorem")
        val mockProfilePicture = null
        // -- mock --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
        whenever(mockRepository.save(any<Profile>())).thenAnswer { it.arguments[0] as Profile }
        // -- execute --
        val result = service.patch(0L, mockRequest, mockProfilePicture)

        // -- capture --
        val profileCaptor = argumentCaptor<Profile>()

        // -- verify --
        assertThat(result).usingRecursiveComparison().isEqualTo(mockProfile)

        verify(mockRepository).save(any<Profile>())
        verify(mockSupabaseService, never()).uploadFile(
            any<String>(),
            any<String>(),
            any<MultipartFile>(),
            any<Boolean>()
        )
        // -- verify the captor --
        verify(mockRepository).save(profileCaptor.capture())
        val profileCaptured = profileCaptor.firstValue
        assertThat(profileCaptured.profilePictureUrl).isEqualTo(existingUrl)
    }

    @Test
    fun `patch WITHOUT profile picture but WITH profilePictureKey uses provided key`() = runTest {
        val mockUser = User("mail", "pw")
        val existingUrl = "old-url.jpg"
        val newUrl = "new-url.jpg"
        val mockProfile = Profile(user = mockUser).apply {
            profilePictureUrl = existingUrl
        }

        val mockRequest = ProfileRequest(name = "Lorem", profilePictureKey = newUrl)
        val mockProfilePicture = null
        // -- mock --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
        whenever(mockRepository.save(any<Profile>())).thenAnswer { it.arguments[0] as Profile }
        // -- execute --
        val result = service.patch(0L, mockRequest, mockProfilePicture)

        // -- capture --
        val profileCaptor = argumentCaptor<Profile>()

        // -- verify --
        verify(mockRepository).save(any<Profile>())
        verify(mockSupabaseService, never()).uploadFile(
            any<String>(),
            any<String>(),
            any<MultipartFile>(),
            any<Boolean>()
        )
        // -- verify the captor --
        verify(mockRepository).save(profileCaptor.capture())
        val profileCaptured = profileCaptor.firstValue
        assertThat(profileCaptured.profilePictureUrl).isEqualTo(newUrl)
    }

    @Test
    fun `patch WITH profile picture ignores profilePictureKey and uploads file`() = runTest {
        val mockUser = User("mail", "pw")
        val existingUrl = "old-url.jpg"
        val requestUrl = "request-url.jpg"
        val uploadedUrl = "uploaded-url.jpg"
        val mockProfile = Profile(user = mockUser).apply {
            profilePictureUrl = existingUrl
        }

        val mockRequest = ProfileRequest(name = "Lorem", profilePictureKey = requestUrl)
        val mockProfilePicture = MockMultipartFile("file", "test.jpg", "image/jpeg", ByteArray(10))
        // -- mock --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
        whenever(
            mockSupabaseService.uploadFile(
                any<String>(),
                any<String>(),
                any<MultipartFile>(),
                any<Boolean>()
            )
        ).thenReturn(uploadedUrl)
        whenever(mockRepository.save(any<Profile>())).thenAnswer { it.arguments[0] as Profile }
        // -- execute --
        val result = service.patch(0L, mockRequest, mockProfilePicture)

        // -- capture --
        val profileCaptor = argumentCaptor<Profile>()

        // -- verify --
        verify(mockRepository).save(any<Profile>())
        verify(mockSupabaseService).uploadFile(
            any<String>(),
            any<String>(),
            any<MultipartFile>(),
            any<Boolean>()
        )
        // -- verify the captor --
        verify(mockRepository).save(profileCaptor.capture())
        val profileCaptured = profileCaptor.firstValue
        assertThat(profileCaptured.profilePictureUrl).isEqualTo(uploadedUrl)
    }

    // -- region: Optimistic Locking Fix Tests --

    @Test
    fun `patch should load profile only once to avoid OptimisticLockingException`() = runTest {
        val mockUser = User("mail", "pw")
        val mockProfile = Profile(user = mockUser)
        val mockRequest = ProfileRequest(name = "Updated Name")

        // -- mock: repository should be called only once for get() --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
        whenever(mockRepository.save(any<Profile>())).thenReturn(mockProfile)

        // -- execute --
        service.patch(1L, mockRequest, null)

        // -- verify: findById should be called exactly once (no double loading) --
        verify(mockRepository, org.mockito.kotlin.times(1)).findById(any<Long>())
        // -- verify: save should be called once --
        verify(mockRepository, org.mockito.kotlin.times(1)).save(any<Profile>())
        // -- verify: userRepository.save should NOT be called (cascade handles it) --
        verify(mockUserRepository, never()).save(any<User>())
    }

    @Test
    fun `patch should update both profile and user fields correctly`() = runTest {
        val mockUser = User("old@email.com", "pw")
        val mockProfile = Profile(user = mockUser).apply {
            name = "Old Name"
            profilePictureUrl = "old-picture.jpg"
        }
        mockUser.mobile = "+1234567890"

        val mockRequest = ProfileRequest(
            name = "New Name",
            mobile = "+9876543210"
        )

        // -- mock --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
        whenever(mockRepository.save(any<Profile>())).thenAnswer { it.arguments[0] as Profile }

        // -- execute --
        val result = service.patch(1L, mockRequest, null)

        // -- verify profile fields updated --
        assertThat(result.name).isEqualTo("New Name")
        // -- verify user fields updated --
        assertThat(result.user.mobile).isEqualTo("+9876543210")
    }

    @Test
    fun `patch with partial update should preserve existing values`() = runTest {
        val mockUser = User("test@email.com", "pw")
        val mockProfile = Profile(user = mockUser).apply {
            name = "Existing Name"
            profilePictureUrl = "existing-picture.jpg"
        }
        mockUser.mobile = "+1111111111"

        // -- Request with only mobile update (name and profilePictureUrl should be preserved) --
        val mockRequest = ProfileRequest(
            mobile = "+2222222222"
        )

        // -- mock --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
        whenever(mockRepository.save(any<Profile>())).thenAnswer { it.arguments[0] as Profile }

        // -- execute --
        val result = service.patch(1L, mockRequest, null)

        // -- verify: name and profilePictureUrl preserved, mobile updated --
        assertThat(result.name).isEqualTo("Existing Name")
        assertThat(result.user.mobile).isEqualTo("+2222222222")
        assertThat(result.profilePictureUrl).isEqualTo("existing-picture.jpg")
    }

    @Test
    fun `update should not call userRepository save separately`() {
        val mockUser = User("test@email.com", "pw")
        val mockProfile = Profile(user = mockUser)
        val mockRequest = ProfileRequest(
            name = "Updated Name",
            mobile = "+3333333333"
        )

        // -- mock --
        whenever(mockRepository.findById(any<Long>())).thenReturn(Optional.of(mockProfile))
        whenever(mockRepository.save(any<Profile>())).thenReturn(mockProfile)

        // -- execute --
        service.update(1L, mockRequest)

        // -- verify: only profile save, no separate user save --
        verify(mockRepository).save(any<Profile>())
        verify(mockUserRepository, never()).save(any<User>())
    }

    // -- end of region: Optimistic Locking Fix Tests --

    // -- region: Mobile Uniqueness Validation Tests --

    @Test
    fun `patch should throw exception when mobile is already in use by another user`() = runTest {
        val mockUser1 = User("user1@email.com", "pw").apply { id = 1L; mobile = "+1111111111" }
        val mockUser2 = User("user2@email.com", "pw").apply { id = 2L; mobile = "+2222222222" }
        val mockProfile1 = Profile(user = mockUser1).apply { name = "User 1" }

        val mockRequest = ProfileRequest(mobile = "+2222222222") // -- trying to use user2's mobile --

        // -- mock --
        whenever(mockRepository.findById(1L)).thenReturn(Optional.of(mockProfile1))
        whenever(mockUserRepository.findByMobile("+2222222222")).thenReturn(mockUser2)

        // -- execute and verify exception --
        val exception = assertThrows<MobileAlreadyExistsException> {
            service.patch(1L, mockRequest, null)
        }

        assertThat(exception.message).contains("+2222222222")
        assertThat(exception.message).contains("already in use")

        // -- verify no save occurred --
        verify(mockRepository, never()).save(any<Profile>())
    }

    @Test
    fun `patch should succeed when mobile is same as current user`() = runTest {
        val mockUser = User("user@email.com", "pw").apply { id = 1L; mobile = "+1111111111" }
        val mockProfile = Profile(user = mockUser).apply { name = "User" }

        val mockRequest = ProfileRequest(mobile = "+1111111111") // -- same mobile --

        // -- mock --
        whenever(mockRepository.findById(1L)).thenReturn(Optional.of(mockProfile))
        whenever(mockRepository.save(any<Profile>())).thenAnswer { it.arguments[0] as Profile }

        // -- execute --
        val result = service.patch(1L, mockRequest, null)

        // -- verify success --
        assertThat(result.user.mobile).isEqualTo("+1111111111")
        verify(mockRepository).save(any<Profile>())
        // -- findByMobile should not be called since mobile hasn't changed --
        verify(mockUserRepository, never()).findByMobile(any())
    }

    @Test
    fun `patch should succeed when mobile is not in use by another user`() = runTest {
        val mockUser = User("user@email.com", "pw").apply { id = 1L; mobile = "+1111111111" }
        val mockProfile = Profile(user = mockUser).apply { name = "User" }

        val mockRequest = ProfileRequest(mobile = "+3333333333") // -- new unique mobile --

        // -- mock --
        whenever(mockRepository.findById(1L)).thenReturn(Optional.of(mockProfile))
        whenever(mockUserRepository.findByMobile("+3333333333")).thenReturn(null) // -- not in use --
        whenever(mockRepository.save(any<Profile>())).thenAnswer { it.arguments[0] as Profile }

        // -- execute --
        val result = service.patch(1L, mockRequest, null)

        // -- verify success --
        assertThat(result.user.mobile).isEqualTo("+3333333333")
        verify(mockRepository).save(any<Profile>())
        verify(mockUserRepository).findByMobile("+3333333333")
    }

    @Test
    fun `update should throw exception when mobile is already in use by another user`() {
        val mockUser1 = User("user1@email.com", "pw").apply { id = 1L; mobile = "+1111111111" }
        val mockUser2 = User("user2@email.com", "pw").apply { id = 2L; mobile = "+2222222222" }
        val mockProfile1 = Profile(user = mockUser1).apply { name = "User 1" }

        val mockRequest = ProfileRequest(name = "Updated Name", mobile = "+2222222222")

        // -- mock --
        whenever(mockRepository.findById(1L)).thenReturn(Optional.of(mockProfile1))
        whenever(mockUserRepository.findByMobile("+2222222222")).thenReturn(mockUser2)

        // -- execute and verify exception --
        val exception = assertThrows<MobileAlreadyExistsException> {
            service.update(1L, mockRequest)
        }

        assertThat(exception.message).contains("+2222222222")
        assertThat(exception.message).contains("already in use")

        // -- verify no save occurred --
        verify(mockRepository, never()).save(any<Profile>())
    }

    @Test
    fun `update should succeed when mobile is not in use by another user`() {
        val mockUser = User("user@email.com", "pw").apply { id = 1L; mobile = "+1111111111" }
        val mockProfile = Profile(user = mockUser).apply { name = "User" }

        val mockRequest = ProfileRequest(name = "Updated Name", mobile = "+3333333333")

        // -- mock --
        whenever(mockRepository.findById(1L)).thenReturn(Optional.of(mockProfile))
        whenever(mockUserRepository.findByMobile("+3333333333")).thenReturn(null)
        whenever(mockRepository.save(any<Profile>())).thenReturn(mockProfile)

        // -- execute --
        val result = service.update(1L, mockRequest)

        // -- verify success --
        assertThat(result.user.mobile).isEqualTo("+3333333333")
        verify(mockRepository).save(any<Profile>())
        verify(mockUserRepository).findByMobile("+3333333333")
    }

    // -- end of region: Mobile Uniqueness Validation Tests --
}
