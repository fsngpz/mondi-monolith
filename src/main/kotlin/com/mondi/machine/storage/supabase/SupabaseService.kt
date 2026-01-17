package com.mondi.machine.storage.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.FileObject
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import kotlin.time.Duration.Companion.seconds

/**
 * The service class of Supabase storage.
 *
 * @author Ferdinand Sangap.
 * @since 2026-01-17
 */
@Service
class SupabaseService(
    @Value("\${supabase.url}") private val supabaseUrl: String,
    @Value("\${supabase.api-key}") private val supabaseKey: String
) {

    private val log = LoggerFactory.getLogger(SupabaseService::class.java)

    companion object {
        const val BUCKET_USERS = "users"
        const val BUCKET_PRODUCTS = "products"
        const val BUCKET_CERTIFICATES = "certificates"
    }

    /**
     * a private function to get Supabase client.
     *
     * @return the [SupabaseClient] instance.
     */
    private fun getClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Storage) {
                transferTimeout = 90.seconds
            }
        }
    }

    /**
     * a function to get all files in the specified bucket.
     *
     * @param bucketName the bucket name.
     * @return the list of [FileObject].
     */
    suspend fun getAllFilesInBucket(bucketName: String): List<FileObject> {
        val supabase = getClient()
        val bucket = supabase.storage.from(bucketName)
        return bucket.list()
    }

    /**
     * a function to upload file to the specified bucket.
     *
     * @param bucketName the bucket name.
     * @param fileName the file name.
     * @param file the [MultipartFile] instance.
     */
    suspend fun uploadFile(
        bucketName: String,
        fileName: String,
        file: MultipartFile,
        isReplaceFileIfExist: Boolean = false
    ): String {
        val supabase = getClient()
        val bucket = supabase.storage.from(bucketName)
        log.info("Uploading $fileName to $bucket")
        val result = bucket.upload(path = fileName, data = file.bytes) {
            upsert = isReplaceFileIfExist
        }
        log.info("Success upload $fileName to $bucket")
        return result.path
    }

    /**
     * a function to get the public url of the file in the specified bucket.
     *
     * @param bucketName the bucket name.
     * @param filePath the file path.
     * @return the public url as [String].
     */
    fun getPublicUrl(bucketName: String, filePath: String): String {
        val supabase = getClient()
        val bucket = supabase.storage.from(bucketName)
        return bucket.publicUrl(filePath)
    }
}
