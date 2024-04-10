package com.mondi.machine.exceptions

/**
 * The model class of response error.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
data class ErrorResponse(
  val type: String,
  val message: String
)