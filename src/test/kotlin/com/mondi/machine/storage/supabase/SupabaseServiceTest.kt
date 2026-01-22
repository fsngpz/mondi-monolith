package com.mondi.machine.storage.supabase

import com.mondi.machine.storage.FileSignedUrlResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.FileObject
import io.github.jan.supabase.storage.FileUploadResponse
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import kotlin.time.Duration.Companion.hours

/**
 * The test class of [SupabaseService].
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-20
 */
internal class SupabaseServiceTest {
    // -- region of mock --
    private lateinit var mockSupabaseClient: SupabaseClient
    private lateinit var mockStorage: Storage
    private lateinit var mockBucket: BucketApi
    private lateinit var supabaseService: SupabaseService
    // -- end of region mock --

    @BeforeEach
    fun setup() {
        mockSupabaseClient = mockk()
        mockStorage = mockk()
        mockBucket = mockk()

        // Mock the storage extension property
        mockkStatic("io.github.jan.supabase.storage.StorageKt")
        every { mockSupabaseClient.storage } returns mockStorage
        every { mockStorage.from(any<String>()) } returns mockBucket

        // Create a spy of the service and override getClient() to return our mock
        supabaseService = spyk(SupabaseService("http://localhost:8000", "test-key"))
        every { supabaseService.getClient() } returns mockSupabaseClient
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    // -- region of smoke testing --
    @Test
    fun `dependencies are not null`() {
        assertThat(supabaseService).isNotNull
    }

    @Test
    fun `constants are defined correctly`() {
        assertThat(SupabaseService.BUCKET_USERS).isEqualTo("users")
        assertThat(SupabaseService.BUCKET_PRODUCTS).isEqualTo("products")
        assertThat(SupabaseService.BUCKET_CERTIFICATES).isEqualTo("certificates")
    }
    // -- end of region smoke testing --

    @Test
    fun `attempting to get all files in bucket and success`() = runTest {
        val bucketName = SupabaseService.BUCKET_USERS
        val mockFiles = listOf(
            createMockFileObject("file1.jpg"),
            createMockFileObject("file2.jpg"),
            createMockFileObject("file3.jpg")
        )
        // -- mock --
        coEvery { mockBucket.list() } returns mockFiles

        // -- execute --
        val result = supabaseService.getAllFilesInBucket(bucketName)

        // -- verify --
        assertThat(result).hasSize(3)
        assertThat(result).isEqualTo(mockFiles)
        coVerify { mockStorage.from(bucketName) }
        coVerify { mockBucket.list() }
    }

    @Test
    fun `attempting to upload file without overwrite and success`() = runTest {
        val bucketName = SupabaseService.BUCKET_CERTIFICATES
        val fileName = "certificate.pdf"
        val mockFile = MockMultipartFile("file", "test.pdf", "application/pdf", ByteArray(1024))
        val uploadedKey = "$bucketName/$fileName"
        val mockUploadData = mockk<FileUploadResponse>()
        // -- mock --
        every { mockUploadData.key } returns uploadedKey
        coEvery { mockBucket.upload(any<String>(), any<ByteArray>(), any()) } returns mockUploadData

        // -- execute --
        val result = supabaseService.uploadFile(bucketName, fileName, mockFile, false)

        // -- verify --
        assertThat(result).isEqualTo(uploadedKey)
        coVerify { mockStorage.from(bucketName) }
        coVerify { mockBucket.upload(eq(fileName), eq(mockFile.bytes), any()) }
    }

    @Test
    fun `attempting to upload file with overwrite and success`() = runTest {
        val bucketName = SupabaseService.BUCKET_PRODUCTS
        val fileName = "product.png"
        val mockFile = MockMultipartFile("file", "product.png", "image/png", ByteArray(2048))
        val uploadedKey = "$bucketName/$fileName"
        val mockUploadData = mockk<FileUploadResponse>()
        // -- mock --
        every { mockUploadData.key } returns uploadedKey
        coEvery { mockBucket.upload(any<String>(), any<ByteArray>(), any()) } returns mockUploadData

        // -- execute --
        val result = supabaseService.uploadFile(bucketName, fileName, mockFile, true)

        // -- verify --
        assertThat(result).isEqualTo(uploadedKey)
        coVerify { mockStorage.from(bucketName) }
        coVerify { mockBucket.upload(eq(fileName), eq(mockFile.bytes), any()) }
    }

    @Test
    fun `attempting to get public url and success`() {
        val bucketName = SupabaseService.BUCKET_USERS
        val filePath = "profile/user123.jpg"
        val fileKey = "$bucketName/$filePath"
        val expectedUrl = "https://supabase.example.com/storage/v1/object/public/$bucketName/$filePath"
        // -- mock --
        every { mockBucket.publicUrl(any<String>()) } returns expectedUrl

        // -- execute --
        val result = supabaseService.getPublicUrl(fileKey)

        // -- verify --
        assertThat(result).isEqualTo(expectedUrl)
        verify { mockStorage.from(bucketName) }
        verify { mockBucket.publicUrl(filePath) }
    }

    @Test
    fun `attempting to get signed url and success`() = runTest {
        val key = "${SupabaseService.BUCKET_CERTIFICATES}/document.pdf"
        val expireDuration = 24.hours
        val expectedSignedUrl =
            "https://supabase.example.com/storage/v1/object/sign/certificates/document.pdf?token=abc123"
        // -- mock --
        coEvery { mockBucket.createSignedUrl(any<String>(), any()) } returns expectedSignedUrl

        // -- execute --
        val result = supabaseService.getSignedUrl(key, expireDuration)

        // -- verify --
        assertThat(result).isInstanceOf(FileSignedUrlResponse::class.java)
        assertThat(result.signedUrl).isEqualTo(expectedSignedUrl)
        assertThat(result.expireAt).isNotNull
        coVerify { mockStorage.from(SupabaseService.BUCKET_CERTIFICATES) }
        coVerify { mockBucket.createSignedUrl("document.pdf", expireDuration) }
    }

    @Test
    fun `attempting to get signed url with nested path and success`() = runTest {
        val key = "${SupabaseService.BUCKET_USERS}/profiles/user123/avatar.png"
        val expireDuration = 1.hours
        val expectedSignedUrl =
            "https://supabase.example.com/storage/v1/object/sign/users/profiles/user123/avatar.png?token=xyz789"
        // -- mock --
        coEvery { mockBucket.createSignedUrl(any<String>(), any()) } returns expectedSignedUrl

        // -- execute --
        val result = supabaseService.getSignedUrl(key, expireDuration)

        // -- verify --
        assertThat(result).isInstanceOf(FileSignedUrlResponse::class.java)
        assertThat(result.signedUrl).isEqualTo(expectedSignedUrl)
        assertThat(result.expireAt).isNotNull
        coVerify { mockStorage.from(SupabaseService.BUCKET_USERS) }
        coVerify { mockBucket.createSignedUrl("profiles/user123/avatar.png", expireDuration) }
    }

    private fun createMockFileObject(name: String): FileObject {
        return mockk<FileObject>().apply {
            every { this@apply.name } returns name
        }
    }
}
