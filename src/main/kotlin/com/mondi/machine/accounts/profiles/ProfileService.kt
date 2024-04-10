package com.mondi.machine.accounts.profiles

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
class ProfileService(private val repository: ProfileRepository) {

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
   * a function to handle request create new profile.
   *
   * @param id the unique identifier of account.
   * @param name the name of account.
   * @param address the address of account.
   * @param profilePicture the [MultipartFile] for profile picture.
   * @return the [Profile] instance.
   */
  fun create(id: Long, name: String, address: String?, profilePicture: MultipartFile?): Profile {
    // -- find the profile to repository by id, if not found then create an instance --
    val profile = repository.findByIdOrNull(id) ?: Profile(name = name).apply {
      this.address = address
    }
    // -- save the instance to database and return --
    return repository.save(profile)
  }
}