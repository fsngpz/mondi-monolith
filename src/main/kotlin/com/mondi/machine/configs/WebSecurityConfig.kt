package com.mondi.machine.configs

import com.mondi.machine.auths.jwt.JwtAuthEntryPoint
import com.mondi.machine.auths.jwt.JwtAuthFilter
import com.mondi.machine.auths.users.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.handler.HandlerMappingIntrospector

/**
 * The configuration class for Web Security.
 *
 * @author Ferdinand Sangap
 * @since 2023-05-27
 */
@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val userService: UserService,
    private val jwtAuthFilter: JwtAuthFilter,
    private val authEntryPoint: JwtAuthEntryPoint
) {

    @Bean
    fun userDetailsService(): UserDetailsService {
        return CustomUserDetailService(userService)
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .securityContext {
                // This ensures context is preserved during async/suspend dispatch
                it.securityContextRepository(RequestAttributeSecurityContextRepository())
            }
            // 1. Explicitly disable CSRF for stateless APIs
            .csrf { it.disable() }

            .exceptionHandling {
                it.authenticationEntryPoint(authEntryPoint)
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests {
                // -- Public: Swagger & Docs --
                it.requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs"
                ).permitAll()

                // -- Public: Auth & Root --
                it.requestMatchers("/v1/auth/**", "/").permitAll()

                // -- Protected: Backoffice --
                it.requestMatchers("/v1/backoffice/**").hasRole(ROLE_ADMIN)

                // -- Protected: Everything else under v1 --
                it.requestMatchers("/v1/**").authenticated()

                // -- Fallback --
                it.anyRequest().authenticated()
            }
            .authenticationProvider(authenticationProvider())
            // 3. Ensure your JWT filter is added correctly
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val authenticationProvider = DaoAuthenticationProvider()
        authenticationProvider.setUserDetailsService(userDetailsService())
        authenticationProvider.setPasswordEncoder(passwordEncoder())
        return authenticationProvider
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

    @Bean(name = ["mvcHandlerMappingIntrospector"])
    fun mvcHandlerMappingIntrospector(): HandlerMappingIntrospector {
        return HandlerMappingIntrospector()
    }

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedOriginPatterns(
                        "*.mondijewellery.studio",
                        "*.mondi-website.vercel.app",
                        "mondijewellery.studio",
                        "https://mondi-website.vercel.app",
                        "https://mondijewellery.studio",
                        "http://localhost:3000",
                        "http://localhost:5173"
                    )
                    .allowedMethods(CorsConfiguration.ALL)
                    .allowedHeaders(CorsConfiguration.ALL)
            }
        }
    }

    companion object {
        const val ROLE_ADMIN = "ADMIN"
    }
}
