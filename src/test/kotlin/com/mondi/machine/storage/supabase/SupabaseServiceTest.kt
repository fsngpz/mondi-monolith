package com.mondi.machine.storage.supabase

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile

/**
 * The test class for [SupabaseService].
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-17
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled
class SupabaseServiceTest(@Autowired private val service: SupabaseService) {

    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
    }

    @Test
    fun getAllFilesInBucket() = runTest {
        // Now you can call suspend functions inside this block
        val result = service.getAllFilesInBucket("my-bucket")

        assertThat(result).isNotEmpty
    }

    @Test
    fun uploadFileToBucket() = runTest {
        val filePath = "file.txt"
        val fileContent = "Hello, Supabase!".toByteArray()

        val mockMultiPartFile = MockMultipartFile(
            "file",
            fileContent
        )
        val result = service.uploadFile("products", filePath, mockMultiPartFile)
        assertThat(result).isNotNull
    }

    @Test
    fun `uploadFileToBucket and getPublicUrl`() = runTest {
        val filePath = "test-upload-file.txt"
        val fileContent = "This is a test file for upload.".toByteArray()

        val mockMultiPartFile = MockMultipartFile(
            "file",
            fileContent
        )
        val uploadResult = service.uploadFile("products", filePath, mockMultiPartFile)
        assertThat(uploadResult).isNotNull

        val publicUrl = service.getPublicUrl("products", filePath)
        assertThat(publicUrl).isNotNull
    }
}
