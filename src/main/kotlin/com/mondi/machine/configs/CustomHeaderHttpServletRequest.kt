package com.mondi.machine.configs

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.util.Enumeration

/**
 * The class to add custom header for [HttpServletRequest].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
class CustomHeaderHttpServletRequest(
  request: HttpServletRequest,
  private val customHeaders: Map<String, String>
) : HttpServletRequestWrapper(request) {

  override fun getHeader(name: String): String? {
    val headerValue = customHeaders[name]
    return headerValue ?: super.getHeader(name)
  }

  override fun getHeaderNames(): Enumeration<String> {
    val originalHeaderNames = super.getHeaderNames()
    val customHeaderNames = customHeaders.keys
    return object : Enumeration<String> {
      override fun hasMoreElements(): Boolean {
        return originalHeaderNames.hasMoreElements() || customHeaderNames.iterator().hasNext()
      }

      override fun nextElement(): String {
        return if (originalHeaderNames.hasMoreElements()) {
          originalHeaderNames.nextElement()
        } else {
          customHeaderNames.iterator().next()
        }
      }
    }
  }
}