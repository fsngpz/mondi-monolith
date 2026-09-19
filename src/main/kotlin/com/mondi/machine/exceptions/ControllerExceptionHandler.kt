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

  /**
   * Handle email verification token not found exception.
   *
   * @param e the [EmailVerificationTokenNotFoundException].
   * @return the [ErrorResponse] with [HttpStatus.NOT_FOUND].
   */
  @ExceptionHandler
  fun handleEmailVerificationTokenNotFoundException(e: EmailVerificationTokenNotFoundException): ResponseEntity<ErrorResponse> {
    // -- setup the instance of error response --
    val errorResponse = ErrorResponse(
      type = e.javaClass.simpleName,
      message = e.message ?: "Email verification token not found"
    )
    // -- return as response entity --
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
  }

  /**
   * Handle invalid email verification token exception.
   *
   * @param e the [InvalidEmailVerificationTokenException].
   * @return the [ErrorResponse] with [HttpStatus.BAD_REQUEST].
   */
  @ExceptionHandler
  fun handleInvalidEmailVerificationTokenException(e: InvalidEmailVerificationTokenException): ResponseEntity<ErrorResponse> {
    // -- setup the instance of error response --
    val errorResponse = ErrorResponse(
      type = e.javaClass.simpleName,
      message = e.message ?: "Invalid email verification token"
    )
    // -- return as response entity --
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
  }

  /**
   * Handle email already verified exception.
   *
   * @param e the [EmailAlreadyVerifiedException].
   * @return the [ErrorResponse] with [HttpStatus.CONFLICT].
   */
  @ExceptionHandler
  fun handleEmailAlreadyVerifiedException(e: EmailAlreadyVerifiedException): ResponseEntity<ErrorResponse> {
    // -- setup the instance of error response --
    val errorResponse = ErrorResponse(
      type = e.javaClass.simpleName,
      message = e.message ?: "Email address is already verified"
    )
    // -- return as response entity --
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
  }

  /**
   * Handle too many requests exception.
   *
   * @param e the [TooManyRequestsException].
   * @return the [ErrorResponse] with [HttpStatus.TOO_MANY_REQUESTS].
   */
  @ExceptionHandler
  fun handleTooManyRequestsException(e: TooManyRequestsException): ResponseEntity<ErrorResponse> {
    // -- setup the instance of error response --
    val errorResponse = ErrorResponse(
      type = e.javaClass.simpleName,
      message = e.message ?: "Too many requests. Please try again later."
    )
    // -- return as response entity --
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse)
  }
}