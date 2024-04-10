import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * The configuration class for bean.
 *
 * @author Ferdinand Sangap
 * @since 2023-05-27
 */
@Configuration
class BeanConfiguration {
  @Bean
  fun bCryptPasswordEncoder(): BCryptPasswordEncoder {
    return BCryptPasswordEncoder()
  }
}