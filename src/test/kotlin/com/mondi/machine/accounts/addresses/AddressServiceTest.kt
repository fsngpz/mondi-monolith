package com.mondi.machine.accounts.addresses

import com.fasterxml.jackson.databind.ObjectMapper
import com.mondi.machine.auths.users.OAuthProvider
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.Optional

/**
 * The test class for [AddressService].
 *
 * @author Ferdinand Sangap
 * @since 2026-01-23
 */
@SpringBootTest(classes = [AddressService::class])
@Import(value = [ObjectMapper::class])
internal class AddressServiceTest(
    @Autowired private val service: AddressService,
    @Autowired private val objectMapper: ObjectMapper
) {
    // -- region of mock --
    @MockitoBean
    lateinit var mockAddressRepository: AddressRepository

    @MockitoBean
    lateinit var mockUserRepository: UserRepository
    // -- end of region mock --

    // -- region of smoke testing --
    @Test
    fun `dependencies are not null`() {
        assertThat(service).isNotNull
        assertThat(mockAddressRepository).isNotNull
        assertThat(mockUserRepository).isNotNull
    }
    // -- end of region smoke testing --

    @Test
    fun `getAllByUserId but user not found`() {
        // -- mock --
        whenever(mockUserRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { service.getAllByUserId(1L) }

        // -- verify --
        verify(mockUserRepository).findById(any<Long>())
        verify(mockAddressRepository, never()).findAllByUser(any())
    }

    @Test
    fun `getAllByUserId and found`() {
        val mockUser = createMockUser()
        val mockAddress1 = createMockAddress(mockUser)
        val mockAddress2 = createMockAddress(mockUser)
        val addresses = listOf(mockAddress1, mockAddress2)

        // -- mock --
        whenever(mockUserRepository.findById(any<Long>())).thenReturn(Optional.of(mockUser))
        whenever(mockAddressRepository.findAllByUser(mockUser)).thenReturn(addresses)

        // -- execute --
        val result = service.getAllByUserId(1L)

        // -- verify --
        assertThat(result).hasSize(2)
        verify(mockUserRepository).findById(any<Long>())
        verify(mockAddressRepository).findAllByUser(mockUser)
    }

    @Test
    fun `getById but address not found`() {
        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { service.getById(1L, 1L) }

        // -- verify --
        verify(mockAddressRepository).findById(any<Long>())
    }

    @Test
    fun `getById but address does not belong to user`() {
        val mockUser = createMockUser()
        mockUser.id = 1L
        val mockAddress = createMockAddress(mockUser)

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.of(mockAddress))

        // -- execute --
        val exception = assertThrows<IllegalArgumentException> { service.getById(1L, 2L) }

        // -- verify --
        assertThat(exception.message).isEqualTo("Address does not belong to user")
        verify(mockAddressRepository).findById(any<Long>())
    }

    @Test
    fun `getById and found`() {
        val mockUser = createMockUser()
        mockUser.id = 1L
        val mockAddress = createMockAddress(mockUser)

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.of(mockAddress))

        // -- execute --
        val result = service.getById(1L, 1L)

        // -- verify --
        assertThat(result).usingRecursiveComparison().isEqualTo(mockAddress)
        verify(mockAddressRepository).findById(any<Long>())
    }

    @Test
    fun `getMainAddress but user not found`() {
        // -- mock --
        whenever(mockUserRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { service.getMainAddress(1L) }

        // -- verify --
        verify(mockUserRepository).findById(any<Long>())
        verify(mockAddressRepository, never()).findByUserAndIsMain(any(), any())
    }

    @Test
    fun `getMainAddress and found`() {
        val mockUser = createMockUser()
        val mockAddress = createMockAddress(mockUser, isMain = true)

        // -- mock --
        whenever(mockUserRepository.findById(any<Long>())).thenReturn(Optional.of(mockUser))
        whenever(mockAddressRepository.findByUserAndIsMain(mockUser, true)).thenReturn(mockAddress)

        // -- execute --
        val result = service.getMainAddress(1L)

        // -- verify --
        assertThat(result).isNotNull
        assertThat(result!!.isMain).isTrue
        verify(mockUserRepository).findById(any<Long>())
        verify(mockAddressRepository).findByUserAndIsMain(mockUser, true)
    }

    @Test
    fun `getMainAddress and not found`() {
        val mockUser = createMockUser()

        // -- mock --
        whenever(mockUserRepository.findById(any<Long>())).thenReturn(Optional.of(mockUser))
        whenever(mockAddressRepository.findByUserAndIsMain(mockUser, true)).thenReturn(null)

        // -- execute --
        val result = service.getMainAddress(1L)

        // -- verify --
        assertThat(result).isNull()
        verify(mockUserRepository).findById(any<Long>())
        verify(mockAddressRepository).findByUserAndIsMain(mockUser, true)
    }

    @Test
    fun `create address but user not found`() {
        val request = createAddressRequest()

        // -- mock --
        whenever(mockUserRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { service.create(1L, request) }

        // -- verify --
        verify(mockUserRepository).findById(any<Long>())
        verify(mockAddressRepository, never()).save(any())
    }

    @Test
    fun `create address with isMain false`() {
        val mockUser = createMockUser()
        val request = createAddressRequest(isMain = false)
        val mockAddress = createMockAddress(mockUser, isMain = false)

        // -- mock --
        whenever(mockUserRepository.findById(any<Long>())).thenReturn(Optional.of(mockUser))
        whenever(mockAddressRepository.save(any<Address>())).thenReturn(mockAddress)

        // -- execute --
        val result = service.create(1L, request)

        // -- verify --
        assertThat(result).usingRecursiveComparison().isEqualTo(mockAddress)
        verify(mockUserRepository).findById(any<Long>())
        verify(mockAddressRepository).save(any<Address>())
        verify(mockAddressRepository, never()).findByUserAndIsMain(any(), any())
    }

    @Test
    fun `create address with isMain true unsets previous main`() {
        val mockUser = createMockUser()
        val request = createAddressRequest(isMain = true)
        val oldMainAddress = createMockAddress(mockUser, isMain = true)
        val newAddress = createMockAddress(mockUser, isMain = true)

        // -- mock --
        whenever(mockUserRepository.findById(any<Long>())).thenReturn(Optional.of(mockUser))
        whenever(mockAddressRepository.findByUserAndIsMain(mockUser, true)).thenReturn(oldMainAddress)
        whenever(mockAddressRepository.save(any<Address>())).thenReturn(newAddress)

        // -- execute --
        val result = service.create(1L, request)

        // -- capture --
        val addressCaptor = argumentCaptor<Address>()

        // -- verify --
        assertThat(result).isNotNull
        verify(mockUserRepository).findById(any<Long>())
        verify(mockAddressRepository).findByUserAndIsMain(mockUser, true)
        verify(mockAddressRepository, org.mockito.kotlin.times(2)).save(addressCaptor.capture())

        // -- verify old main was unset --
        assertThat(oldMainAddress.isMain).isFalse
    }

    @Test
    fun `update address but address not found`() {
        val request = createAddressRequest()

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { service.update(1L, 1L, request) }

        // -- verify --
        verify(mockAddressRepository).findById(any<Long>())
    }

    @Test
    fun `update address successfully`() {
        val mockUser = createMockUser()
        mockUser.id = 1L
        val mockAddress = createMockAddress(mockUser)
        val request = createAddressRequest(
            street = "Updated Street",
            city = "Updated City"
        )

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.of(mockAddress))
        whenever(mockAddressRepository.save(any<Address>())).thenReturn(mockAddress)

        // -- execute --
        val result = service.update(1L, 1L, request)

        // -- verify --
        assertThat(result.street).isEqualTo("Updated Street")
        assertThat(result.city).isEqualTo("Updated City")
        verify(mockAddressRepository).findById(any<Long>())
        verify(mockAddressRepository).save(mockAddress)
    }

    @Test
    fun `update address with isMain true unsets previous main`() {
        val mockUser = createMockUser()
        mockUser.id = 1L
        val oldMainAddress = createMockAddress(mockUser, isMain = true)
        val currentAddress = createMockAddress(mockUser, isMain = false)
        val request = createAddressRequest(isMain = true)

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.of(currentAddress))
        whenever(mockAddressRepository.findByUserAndIsMain(mockUser, true)).thenReturn(oldMainAddress)
        whenever(mockAddressRepository.save(any<Address>())).thenReturn(currentAddress)

        // -- execute --
        val result = service.update(1L, 1L, request)

        // -- verify --
        assertThat(result.isMain).isTrue
        assertThat(oldMainAddress.isMain).isFalse
        verify(mockAddressRepository).findById(any<Long>())
        verify(mockAddressRepository).findByUserAndIsMain(mockUser, true)
        verify(mockAddressRepository, org.mockito.kotlin.times(2)).save(any<Address>())
    }

    @Test
    fun `patch address successfully with partial update`() {
        val mockUser = createMockUser()
        mockUser.id = 1L
        val mockAddress = createMockAddress(mockUser)
        mockAddress.street = "Old Street"
        mockAddress.city = "Old City"
        mockAddress.country = "Old Country"
        mockAddress.label = "Old Label"

        // -- partial request with only street and label updated --
        // -- note: ObjectMapper will merge this with existing values --
        val partialRequestJson = objectMapper.createObjectNode().apply {
            put("street", "New Street")
            put("label", "New Label")
        }

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.of(mockAddress))
        whenever(mockAddressRepository.save(any<Address>())).thenReturn(mockAddress)

        // -- execute --
        val result = service.patch(1L, 1L, partialRequestJson)

        // -- verify that only street and label were updated, other fields remain unchanged --
        assertThat(result.street).isEqualTo("New Street")
        assertThat(result.city).isEqualTo("Old City")
        assertThat(result.country).isEqualTo("Old Country")
        assertThat(result.label).isEqualTo("New Label")
        verify(mockAddressRepository, org.mockito.kotlin.times(2)).findById(any<Long>())
        verify(mockAddressRepository).save(mockAddress)
    }

    @Test
    fun `delete address but address not found`() {
        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { service.delete(1L, 1L) }

        // -- verify --
        verify(mockAddressRepository).findById(any<Long>())
        verify(mockAddressRepository, never()).delete(any())
    }

    @Test
    fun `delete address but it is main address`() {
        val mockUser = createMockUser()
        mockUser.id = 1L
        val mockAddress = createMockAddress(mockUser, isMain = true)

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.of(mockAddress))

        // -- execute --
        val exception = assertThrows<IllegalStateException> { service.delete(1L, 1L) }

        // -- verify --
        assertThat(exception.message).isEqualTo("Cannot delete main address. Please set another address as main first.")
        verify(mockAddressRepository).findById(any<Long>())
        verify(mockAddressRepository, never()).delete(any())
    }

    @Test
    fun `delete address successfully`() {
        val mockUser = createMockUser()
        mockUser.id = 1L
        val mockAddress = createMockAddress(mockUser, isMain = false)

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.of(mockAddress))

        // -- execute --
        service.delete(1L, 1L)

        // -- verify --
        verify(mockAddressRepository).findById(any<Long>())
        verify(mockAddressRepository).delete(mockAddress)
    }

    @Test
    fun `setAsMain but address not found`() {
        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.empty())

        // -- execute --
        assertThrows<NoSuchElementException> { service.setAsMain(1L, 1L) }

        // -- verify --
        verify(mockAddressRepository).findById(any<Long>())
        verify(mockAddressRepository, never()).save(any())
    }

    @Test
    fun `setAsMain but already main address`() {
        val mockUser = createMockUser()
        mockUser.id = 1L
        val mockAddress = createMockAddress(mockUser, isMain = true)

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.of(mockAddress))

        // -- execute --
        val result = service.setAsMain(1L, 1L)

        // -- verify --
        assertThat(result.isMain).isTrue
        verify(mockAddressRepository).findById(any<Long>())
        verify(mockAddressRepository, never()).findByUserAndIsMain(any(), any())
        verify(mockAddressRepository, never()).save(any())
    }

    @Test
    fun `setAsMain successfully unsets previous main`() {
        val mockUser = createMockUser()
        mockUser.id = 1L
        val oldMainAddress = createMockAddress(mockUser, isMain = true)
        val newMainAddress = createMockAddress(mockUser, isMain = false)

        // -- mock --
        whenever(mockAddressRepository.findById(any<Long>())).thenReturn(Optional.of(newMainAddress))
        whenever(mockAddressRepository.findByUserAndIsMain(mockUser, true)).thenReturn(oldMainAddress)
        whenever(mockAddressRepository.save(any<Address>())).thenReturn(newMainAddress)

        // -- execute --
        val result = service.setAsMain(1L, 1L)

        // -- verify --
        assertThat(result.isMain).isTrue
        assertThat(oldMainAddress.isMain).isFalse
        verify(mockAddressRepository).findById(any<Long>())
        verify(mockAddressRepository).findByUserAndIsMain(mockUser, true)
        verify(mockAddressRepository, org.mockito.kotlin.times(2)).save(any<Address>())
    }

    // -- region of helper functions --
    private fun createMockUser(): User {
        return User(
            email = "test@example.com",
            password = "password",
            provider = OAuthProvider.LOCAL
        )
    }

    private fun createMockAddress(user: User, isMain: Boolean = false): Address {
        return Address(
            user = user,
            street = "123 Test St",
            city = "Test City",
            state = "Test State",
            postalCode = "12345",
            country = "Test Country",
            tag = AddressTag.HOME,
            isMain = isMain
        ).apply {
            this.label = "Test Label"
            this.notes = "Test Notes"
        }
    }

    private fun createAddressRequest(
        street: String = "123 Test St",
        city: String = "Test City",
        state: String = "Test State",
        postalCode: String = "12345",
        country: String = "Test Country",
        tag: AddressTag = AddressTag.HOME,
        isMain: Boolean = false,
        label: String? = "Test Label",
        notes: String? = "Test Notes"
    ): AddressRequest {
        return AddressRequest(
            street = street,
            city = city,
            state = state,
            postalCode = postalCode,
            country = country,
            tag = tag,
            isMain = isMain,
            label = label,
            notes = notes
        )
    }
    // -- end of region helper functions --
}
