package com.mondi.machine.accounts.profiles

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.mondi.machine.auths.users.User
import com.mondi.machine.storage.supabase.SupabaseService
import com.mondi.machine.storage.supabase.SupabaseService.Companion.BUCKET_USERS
import com.mondi.machine.utils.MobileNumberValidator
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
     * @param profilePictureUrl the profile picture url.
     * @return the created [Profile] instance.
     */
    fun create(user: User, profilePictureUrl: String?): Profile {
        // -- setup instance Profile --
        val profile = Profile(user).apply {
            this.profilePictureUrl = profilePictureUrl
        }
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
        // -- validate and normalize mobile number if provided --
        val normalizedMobile = MobileNumberValidator.validateAndNormalize(request.mobile)
        // -- get the profile instance --
        val profile = get(id)
        profile.name = request.name
        profile.profilePictureUrl = request.profilePictureKey
        // -- update user fields --
        profile.user.mobile = normalizedMobile
        profile.user.membershipSince = request.membershipSince
        // -- save the profile instance (cascade will handle user) --
        return repository.save(profile)
    }

    /**
     * a function to handle request patch / partial update of instance [Profile].
     *
     * FIXED: Removed double loading of Profile entity to prevent OptimisticLockingFailureException.
     * FIXED: Updates profile directly without calling update() which would reload the entity.
     *
     * Handles profile picture updates:
     * 1. If MultipartFile is provided: upload and use the new URL
     * 2. If profilePictureKey is provided in request: use that URL
     * 3. Otherwise: preserve existing profile picture
     *
     * @param id the unique identifier.
     * @param request the [ProfileRequest] of payload.
     * @param profilePicture the profile picture file.
     * @return the [Profile] instance.
     */
    suspend fun patch(id: Long, request: ProfileRequest, profilePicture: MultipartFile?): Profile {
        // -- get the profile instance (load once) --
        val profile = get(id)

        // -- convert the request to json node to check which fields are present --
        val jsonNode = objectMapper.convertValue<JsonNode>(request)

        // -- upload profile picture if provided and update JSON --
        val updatedJsonNode = jsonNode.uploadProfilePicture(id, profilePicture)

        // -- update fields that are explicitly provided in the request --
        if (updatedJsonNode.has("name") && !updatedJsonNode["name"].isNull) {
            profile.name = updatedJsonNode["name"].asText()
        }

        // -- validate name is not null after update --
        requireNotNull(profile.name) {
            "field 'name' cannot be null"
        }

        // -- update profilePictureUrl if file was uploaded or profilePictureKey was provided --
        if (profilePicture != null) {
            // -- file was uploaded, use the new key from JSON --
            profile.profilePictureUrl = updatedJsonNode["profilePictureKey"].asText()
        } else if (updatedJsonNode.has("profilePictureKey") && !updatedJsonNode["profilePictureKey"].isNull) {
            // -- profilePictureKey was explicitly provided in request and is not null --
            profile.profilePictureUrl = updatedJsonNode["profilePictureKey"].asText()
        }
        // -- if neither, profilePictureUrl is preserved (not updated) --

        // -- update mobile if provided --
        if (updatedJsonNode.has("mobile")) {
            val mobileValue = if (updatedJsonNode["mobile"].isNull) null else updatedJsonNode["mobile"].asText()
            val normalizedMobile = MobileNumberValidator.validateAndNormalize(mobileValue)
            profile.user.mobile = normalizedMobile
        }

        // -- update membershipSince if provided --
        if (updatedJsonNode.has("membershipSince") && !updatedJsonNode["membershipSince"].isNull) {
            val membershipSinceStr = updatedJsonNode["membershipSince"].asText()
            profile.user.membershipSince = java.time.OffsetDateTime.parse(membershipSinceStr)
        }

        // -- save the profile instance (cascade will handle user) --
        return repository.save(profile)
    }

    /**
     * a private function to upload profile picture and update the JSON node with the profile picture key.
     *
     * Handles three scenarios:
     * 1. If MultipartFile is provided: upload new file and set the new URL in the JSON
     * 2. If profilePictureKey is provided in request: keep it as is
     * 3. If neither is provided: don't modify the JSON (field won't be present in merge)
     *
     * @param id the profile unique identifier.
     * @param profilePicture the [MultipartFile] of Profile Picture.
     * @return the modified [JsonNode] with Profile Picture Key if applicable.
     */
    private suspend fun JsonNode.uploadProfilePicture(
        id: Long,
        profilePicture: MultipartFile?
    ): JsonNode {
        // -- if a file is provided, upload it and update the JSON node --
        return if (profilePicture != null) {
            val extension = profilePicture.originalFilename?.substringAfterLast('.', "")
            val uploadedKey = supabaseService.uploadFile(
                bucketName = BUCKET_USERS,
                fileName = "/profile-picture/user-${id}.${extension}",
                file = profilePicture,
                isOverwriteFile = true
            )
            // -- create a mutable copy of the JSON node and set the profilePictureKey --
            val mutableNode = (this as com.fasterxml.jackson.databind.node.ObjectNode).deepCopy()
            mutableNode.put("profilePictureKey", uploadedKey)
            mutableNode
        } else {
            // -- no file provided, return the original JSON node as is --
            this
        }
    }
}
