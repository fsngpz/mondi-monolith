package com.mondi.machine.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * The class for controller exception handler.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@ControllerAdvice
class ControllerExceptionHandler {

  /**
   * Handle general exception.
   *
   * @param e the [Exception].
   * @return the [ErrorResponse] with [HttpStatus.INTERNAL_SERVER_ERROR].
   */
  @ExceptionHandler
  fun handleGeneralException(e: Exception): ResponseEntity<ErrorResponse> {
    // -- setup the instance of error response --
    val errorResponse = ErrorResponse(
      type = e.javaClass.simpleName,
      message = e.message ?: e.stackTraceToString()
    )
    // -- return as response entity --
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
  }

  /**
   * Handlle authentication exception.
   *
   * @param e the [AuthenticationException].
   * @return the [ErrorResponse] with [HttpStatus.UNAUTHORIZED].
   */
  @ExceptionHandler
  fun handlleAuthenticationException(e: AuthenticationException): ResponseEntity<ErrorResponse> {
    // -- setup the instance of error response --
    val errorResponse = ErrorResponse(
      type = e.javaClass.simpleName,
      message = e.message ?: e.stackTraceToString()
    )    // -- return as response entity --
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
  }

  /**
   * Handle mobile already exists exception.
   *
   * @param e the [MobileAlreadyExistsException].
   * @return the [ErrorResponse] with [HttpStatus.CONFLICT].
   */
  @ExceptionHandler
  fun handleMobileAlreadyExistsException(e: MobileAlreadyExistsException): ResponseEntity<ErrorResponse> {
    // -- setup the instance of error response --
    val errorResponse = ErrorResponse(
      type = e.javaClass.simpleName,
      message = e.message ?: "Mobile number is already in use"
    )
    // -- return as response entity --
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
  }

  /**
   * Handle email send exception.
   *
   * @param e the [EmailSendException].
   * @return the [ErrorResponse] with [HttpStatus.SERVICE_UNAVAILABLE].
   */
  @ExceptionHandler
  fun handleEmailSendException(e: EmailSendException): ResponseEntity<ErrorResponse> {
    // -- setup the instance of error response --
    val errorResponse = ErrorResponse(
      type = e.javaClass.simpleName,
      message = e.message ?: "Failed to send email"
    )
    // -- return as response entity --
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
  }
}