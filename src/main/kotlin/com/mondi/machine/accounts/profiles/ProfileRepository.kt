package com.mondi.machine.accounts.profiles

import org.springframework.data.jpa.repository.JpaRepository

/**
 * The interface for [Profile] database.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
interface ProfileRepository : JpaRepository<Profile, Long> {

    /**
     * a function to check whether the profile's picture is stored under the given key.
     *
     * @param id the profile / user unique identifier.
     * @param profilePictureUrl the storage key.
     * @return true if it is.
     */
    fun existsByIdAndProfilePictureUrl(id: Long, profilePictureUrl: String): Boolean
}