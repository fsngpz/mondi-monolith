package com.mondi.machine.accounts.profiles

import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import com.mondi.machine.auths.users.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * The test class of [NewProfileEventListener].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-12
 */
@SpringBootTest(classes = [NewProfileEventListener::class])
internal class NewProfileEventListenerTest(
    @Autowired private val listener: NewProfileEventListener
) {
    // -- region of mock --
    @MockitoBean
    lateinit var mockProfileService: ProfileService

    @MockitoBean
    lateinit var mockUserService: UserService
    // -- end of region mock --

    // -- region of smoke test --
    @Test
    fun `dependencies are not null`() {
        assertThat(listener).isNotNull
        assertThat(mockProfileService).isNotNull
        assertThat(mockUserService).isNotNull
    }
    // -- end of region smoke test --

    @Test
    fun `onApplicationEvent success`() {
        // -- mock --
        val mockUser = User("email", "pass").apply { this.id = 1L }
        whenever(mockUserService.get(1L)).thenReturn(mockUser)

        val mockRequest = UserEventRequest.from(mockUser, profilePictureUrl = "http://profile.picture.url/image.png")
        val mockEvent = UserApplicationEvent(mockRequest)

        // -- execute --
        listener.onApplicationEvent(mockEvent)

        // -- verify --
        verify(mockUserService).get(1L)
        verify(mockProfileService).create(any<User>(), any<String>())
    }
}
