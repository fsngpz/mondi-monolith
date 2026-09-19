package com.mondi.machine.configs

import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for email-related beans.
 *
 * @author Ferdinand Sangap
 * @since 2026-01-31
 */
@Configuration
class EmailConfig {

    /**
     * Configure and create VelocityEngine bean for template processing.
     *
     * @return configured [VelocityEngine] instance.
     */
    @Bean
    fun velocityEngine(): VelocityEngine {
        val velocityEngine = VelocityEngine()

        // -- set the resource loader to classpath --
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
        velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.name)

        // -- set encoding --
        velocityEngine.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8")
        velocityEngine.setProperty("resource.default_encoding", "UTF-8")

        // -- initialize the engine --
        velocityEngine.init()

        return velocityEngine
    }
}
