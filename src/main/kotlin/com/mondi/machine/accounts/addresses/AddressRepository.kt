package com.mondi.machine.accounts.addresses

import com.mondi.machine.auths.users.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * The repository interface for Address entity.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@Repository
interface AddressRepository : JpaRepository<Address, Long> {

    /**
     * Find all addresses for a user.
     *
     * @param user the [User] instance.
     * @return list of [Address].
     */
    fun findAllByUser(user: User): List<Address>

    /**
     * Find main address for a user.
     *
     * @param user the [User] instance.
     * @param isMain the main flag.
     * @return the main [Address] if found, null otherwise.
     */
    fun findByUserAndIsMain(user: User, isMain: Boolean): Address?

    /**
     * Count addresses for a user.
     *
     * @param user the [User] instance.
     * @return number of addresses.
     */
    fun countByUser(user: User): Long
}
