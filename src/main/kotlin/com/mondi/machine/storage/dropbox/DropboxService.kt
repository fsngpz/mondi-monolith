package com.mondi.machine.storage.dropbox

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * The service class for Dropbox.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-11
 */
@Service
class DropboxService(
  @Value("\${dropbox.access-token}") private val dropboxAccessToken: String,
  @Value("\${dropbox.refresh-token}") private val dropboxRefreshToken: String,
  @Value("\${dropbox.app-key}") private val dropboxAppKey: String,
  @Value("\${dropbox.app-secret}") private val dropboxAppSecret: String
) {

  /**
   * a private function to setting up the [DbxClientV2].
   *
   * @return the [DbxClientV2] instance.
   */
  private fun getClient(): DbxClientV2 {
    // -- setup the config --
    val config = DbxRequestConfig.newBuilder("dropbox/mondi").build()
    // -- setup the DBX Credential --
    val credential = DbxCredential(
      dropboxAccessToken,
      Long.MAX_VALUE,
      dropboxRefreshToken,
      dropboxAppKey,
      dropboxAppSecret
    )
    return DbxClientV2(config, credential)
  }

  /**
   * a function to handle upload the [MultipartFile] to Dropbox.
   *
   * @param userId the user unique identifier.
   * @param file the [MultipartFile] of file.
   * @return the [String] url of uploaded file.
   */
  fun upload(userId: Long, file: MultipartFile): String {
    // -- get the client --
    val client = getClient()
    // -- upload the file --
    val result = client.files().uploadBuilder(
      "/$userId/${file.originalFilename}"
    ).uploadAndFinish(file.inputStream)
    // -- return the shared link --
    return client.sharing().createSharedLinkWithSettings(result.pathLower).url
  }
}