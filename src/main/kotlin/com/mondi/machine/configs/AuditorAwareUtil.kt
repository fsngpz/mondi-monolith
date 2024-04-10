package com.mondi.machine.configs

import jakarta.servlet.http.HttpServletRequest
import java.util.Optional
import org.springframework.data.domain.AuditorAware
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * The util class for Auditor Aware.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
class AuditorAwareUtil : AuditorAware<String> {

  override fun getCurrentAuditor(): Optional<String> {
    val httpServletRequest = getCurrentRequester()

    // -- get the creator from header 'Account-UUID' or else 'SYSTEM' --
    val creator = if (httpServletRequest == null) {
      DEFAULT_CREATOR
    } else {
      httpServletRequest.getHeader(ID)
    }

    return Optional.of(creator ?: DEFAULT_CREATOR)
  }

  /**
   * a private function to get the current requester from the [RequestContextHolder].
   *
   * @return the [HttpServletRequest] or null.
   */
  private fun getCurrentRequester(): HttpServletRequest? {
    val requester = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes?

    return requester?.request
  }

  companion object {
    const val DEFAULT_CREATOR = "SYSTEM"
    const val ID = "ID"
  }
}