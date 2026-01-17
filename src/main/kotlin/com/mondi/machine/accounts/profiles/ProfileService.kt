package com.mondi.machine.accounts.profiles

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.mondi.machine.auths.users.User
import com.mondi.machine.storage.supabase.SupabaseService
import com.mondi.machine.storage.supabase.SupabaseService.Companion.BUCKET_USERS
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * The service class for [Profile].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
@Service
class ProfileService(
    private val objectMapper: ObjectMapper,
    private val supabaseService: SupabaseService,
    private val repository: ProfileRepository
) {

    /**
     * a function to handle request retrieving instance of [Profile].
     *
     * @param id the unique identifier of profile.
     * @return the [Profile] instance.
     */
    fun get(id: Long): Profile {
        // -- find the Profile by id or else throw an exception --
        return repository.findByIdOrNull(id) ?: throw NoSuchElementException(
            "no profile was found with id '$id'"
        )
    }

    /**
     * a function to handle request create new [Profile] instance.
     *
     * @param user the [User] instance.
     * @return the created [Profile] instance.
     */
    fun create(user: User): Profile {
        // -- setup instance Profile --
        val profile = Profile(user)
        // -- save the instance to database --
        return repository.save(profile)
    }

    /**
     * a function to update the instance of [Profile].
     *
     * @param id the [Profile] unique identifier.
     * @param request the [ProfileRequest] instance.
     * @return the [Profile] instance.
     */
    fun update(id: Long, request: ProfileRequest): Profile {
        // -- validate the field 'name' --
        requireNotNull(request.name) {
            "field 'name' cannot be null"
        }
        // -- get the profile instance --
        val profile = get(id).apply {
            this.name = request.name
            this.address = request.address
            this.profilePictureUrl = request.profilePictureUrl
        }
        // -- save the instance --
        return repository.save(profile)
    }

    /**
     * a function to handle request patch / partial update of instance [Profile].
     *
     * @param id the unique identifier.
     * @param request the [JsonNode] of payload.
     * @param profilePicture the profile picture url.
     * @return the [Profile] instance.
     */
    suspend fun patch(id: Long, request: ProfileRequest, profilePicture: MultipartFile?): Profile {
        // -- convert the request to json node --
        val jsonNode = objectMapper.convertValue<JsonNode>(request)
        // -- get the profile instance --
        val profile = get(id)
        // -- convert the instance of Profile to ProfileRequest --
        val body = profile.toRequest()
        // -- read for updating --
        val reader = objectMapper.readerForUpdating(body)
        // -- upload the profile picture --
        val newRequest = jsonNode.uploadProfilePicture(id, profilePicture)
        // -- merge the instance --
        val mergedInstance = reader.readValue<ProfileRequest>(newRequest)
        // -- update the instance --
        return update(id, mergedInstance)
    }

    /**
     * a private function to upload profile picture and store it to the new request then return as
     * JSON Node.
     *
     * @param id the profile unique identifier.
     * @param profilePicture the [MultipartFile] of Profile Picture.
     * @return the new [JsonNode] with Profile Picture URL.
     */
    private suspend fun JsonNode.uploadProfilePicture(
        id: Long,
        profilePicture: MultipartFile?
    ): JsonNode {
        // -- upload profile picture if the profilePicture is null then the value will be null --
        val profilePictureUrl = profilePicture?.let {
            supabaseService.uploadFile(
                bucketName = BUCKET_USERS, "/profile-picture/${id}", it
            )
        }
        // -- convert the value of JsonNode to ProfileRequest --
        val nodeRequest = objectMapper.convertValue<ProfileRequest>(this)
        // -- create new instance ProfileRequest and add the profile picture url to it --
        val newRequest = ProfileRequest(nodeRequest.name, nodeRequest.address, profilePictureUrl)
        // -- return as the JsonNode --
        return objectMapper.convertValue<JsonNode>(newRequest)
    }
}
