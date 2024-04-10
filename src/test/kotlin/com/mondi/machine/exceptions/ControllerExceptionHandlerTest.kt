package com.mondi.machine.exceptions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus

/**
 * The test class for [ControllerExceptionHandler].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@SpringBootTest(classes = [ControllerExceptionHandler::class])
internal class ControllerExceptionHandlerTest(
  @Autowired
  private val controllerExceptionHandler: ControllerExceptionHandler
) {
  // -- region of smoke test --
  @Test
  fun `dependency is not null`() {
    assertThat(controllerExceptionHandler).isNotNull
  }
  // -- end of region smoke test --

  @Test
  fun `handle general exception`() {
    val exception = Exception("test throw an exception")
    // -- execute --
    val result = controllerExceptionHandler.handleGeneralException(exception)
    // -- verify --
    assertThat(result.body?.message).isEqualTo(exception.message)
    assertThat(result.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
  }
}