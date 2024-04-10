package com.mondi.machine.accounts.profiles

import org.springframework.data.jpa.repository.JpaRepository

/**
 * The interface for [Profile] database.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
interface ProfileRepository : JpaRepository<Profile, Long>