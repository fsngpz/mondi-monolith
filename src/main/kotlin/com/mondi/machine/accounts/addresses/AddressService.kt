package com.mondi.machine.accounts.addresses

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The service class for Address operations.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@Service
class AddressService(
    private val addressRepository: AddressRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger: Logger = LoggerFactory.getLogger(AddressService::class.java)

    /**
     * Get all addresses for a user.
     *
     * @param userId the user ID.
     * @return list of [Address].
     */
    fun getAllByUserId(userId: Long): List<Address> {
        // -- find user --
        val user = userRepository.findByIdOrNull(userId)
            ?: throw NoSuchElementException("User not found with id: $userId")
        // -- return all addresses --
        return addressRepository.findAllByUser(user)
    }

    /**
     * Get a specific address by ID.
     *
     * @param addressId the address ID.
     * @param userId the user ID.
     * @return the [Address] instance.
     */
    fun getById(addressId: Long, userId: Long): Address {
        // -- find address --
        val address = addressRepository.findByIdOrNull(addressId)
            ?: throw NoSuchElementException("Address not found with id: $addressId")
        // -- verify ownership --
        require(address.user.id == userId) {
            "Address does not belong to user"
        }
        return address
    }

    /**
     * Get main address for a user.
     *
     * @param userId the user ID.
     * @return the main [Address] if found, null otherwise.
     */
    fun getMainAddress(userId: Long): Address? {
        // -- find user --
        val user = userRepository.findByIdOrNull(userId)
            ?: throw NoSuchElementException("User not found with id: $userId")
        // -- return main address --
        return addressRepository.findByUserAndIsMain(user, true)
    }

    /**
     * Create a new address for a user.
     *
     * @param userId the user ID.
     * @param request the [AddressRequest] instance.
     * @return the created [Address] instance.
     */
    @Transactional
    fun create(userId: Long, request: AddressRequest): Address {
        // -- validate required fields --
        requireNotNull(request.street) { "field 'street' cannot be null" }
        requireNotNull(request.city) { "field 'city' cannot be null" }
        requireNotNull(request.country) { "field 'country' cannot be null" }

        // -- find user --
        val user = userRepository.findByIdOrNull(userId)
            ?: throw NoSuchElementException("User not found with id: $userId")

        // -- if this address is marked as main, unset other main addresses --
        val isMain = request.isMain ?: false
        if (isMain) {
            unsetMainAddress(user)
        }

        // -- create new address --
        val address = Address(
            user = user,
            street = request.street,
            city = request.city,
            state = request.state,
            postalCode = request.postalCode,
            country = request.country,
            tag = request.tag ?: AddressTag.HOME,
            isMain = isMain
        ).apply {
            this.label = request.label
            this.notes = request.notes
        }

        // -- save and return --
        val savedAddress = addressRepository.save(address)
        logger.info("Created address ${savedAddress.id} for user $userId")
        return savedAddress
    }

    /**
     * Update an existing address.
     *
     * @param addressId the address ID.
     * @param userId the user ID.
     * @param request the [AddressRequest] instance.
     * @return the updated [Address] instance.
     */
    @Transactional
    fun update(addressId: Long, userId: Long, request: AddressRequest): Address {
        // -- get existing address --
        val address = getById(addressId, userId)

        // -- if changing to main, unset other main addresses --
        val newIsMain = request.isMain
        if (newIsMain && !address.isMain) {
            unsetMainAddress(address.user)
        }

        // -- update fields --
        address.apply {
            this.street = request.street
            this.city = request.city
            this.state = request.state
            this.postalCode = request.postalCode
            this.country = request.country
            this.tag = request.tag
            this.isMain = newIsMain
            this.label = request.label
            this.notes = request.notes
        }

        // -- save and return --
        val updatedAddress = addressRepository.save(address)
        logger.info("Updated address $addressId for user $userId")
        return updatedAddress
    }

    /**
     * Patch / partial update an existing address.
     *
     * @param addressId the address ID.
     * @param userId the user ID.
     * @param request the [AddressRequest] instance with partial updates.
     * @return the updated [Address] instance.
     */
    @Transactional
    fun patch(addressId: Long, userId: Long, request: JsonNode): Address {
        // -- get the existing address --
        val address = getById(addressId, userId)
        // -- convert the instance of Address to AddressRequest --
        val body = address.toNullableRequest()
        // -- read for updating --
        val reader = objectMapper.readerForUpdating(body)
        // -- merge the instance --
        val mergedInstance = reader.readValue<AddressNullableRequest>(request)
        // -- update the instance --
        return update(addressId, userId, mergedInstance.toNonNull())
    }

    /**
     * Delete an address.
     *
     * @param addressId the address ID.
     * @param userId the user ID.
     */
    @Transactional
    fun delete(addressId: Long, userId: Long) {
        // -- get existing address --
        val address = getById(addressId, userId)

        // -- check if it's the main address --
        if (address.isMain) {
            throw IllegalStateException("Cannot delete main address. Please set another address as main first.")
        }

        // -- delete address --
        addressRepository.delete(address)
        logger.info("Deleted address $addressId for user $userId")
    }

    /**
     * Set an address as main address.
     *
     * @param addressId the address ID.
     * @param userId the user ID.
     * @return the updated [Address] instance.
     */
    @Transactional
    fun setAsMain(addressId: Long, userId: Long): Address {
        // -- get existing address --
        val address = getById(addressId, userId)

        // -- if already main, return --
        if (address.isMain) {
            return address
        }

        // -- unset other main addresses --
        unsetMainAddress(address.user)

        // -- set as main --
        address.isMain = true

        // -- save and return --
        val updatedAddress = addressRepository.save(address)
        logger.info("Set address $addressId as main for user $userId")
        return updatedAddress
    }

    /**
     * Unset main address for a user.
     *
     * @param user the [User] instance.
     */
    private fun unsetMainAddress(user: User) {
        val currentMain = addressRepository.findByUserAndIsMain(user, true)
        if (currentMain != null) {
            currentMain.isMain = false
            addressRepository.save(currentMain)
            logger.info("Unset main address ${currentMain.id} for user ${user.id}")
        }
    }
}
