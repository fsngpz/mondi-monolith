package com.mondi.machine.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * The configuration class for Auditing JPA.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class AuditorConfig {

  /**
   * a bean method for auditor aware.
   *
   * @return the [AuditorAware].
   */
  @Bean
  fun auditorAware(): AuditorAware<String>{
    return AuditorAwareUtil()
  }
}