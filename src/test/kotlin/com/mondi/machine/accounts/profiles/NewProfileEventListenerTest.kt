package com.mondi.machine.accounts.profiles

import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserApplicationEvent
import com.mondi.machine.auths.users.UserEventRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

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
  @MockBean
  lateinit var mockProfileService: ProfileService
  // -- end of region mock --

  // -- region of smoke test --
  @Test
  fun `dependencies are not null`() {
    assertThat(listener).isNotNull
    assertThat(mockProfileService).isNotNull
  }
  // -- end of region smoke test --

  @Test
  fun `onApplicationEvent success`() {
    // -- mock --
    val mockUser = User("email", "pass").apply { this.id = 1L }
    val mockRequest = UserEventRequest(mockUser)
    val mockEvent = UserApplicationEvent(mockRequest)

    // -- execute --
    listener.onApplicationEvent(mockEvent)

    // -- verify --
    verify(mockProfileService).create(any<User>())
  }
}