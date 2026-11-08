package com.mondi.machine.storage.supabase

import io.github.jan.supabase.storage.FileObject
import kotlinx.serialization.KSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Guards against running supabase-kt with an older kotlinx-serialization than it was built for.
 *
 * Spring Boot's dependency management pins kotlinx-serialization (1.6.3 in Boot 3.5). supabase-kt
 * 3.3.0 and Ktor 3.3 are built against 1.9.0, and on 1.6.3 every upload failed at runtime with
 * `AbstractMethodError: GeneratedSerializer.typeParametersSerializers()`. The storage tests mock
 * Supabase, so only touching a real supabase-kt serializer shows it.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
internal class SupabaseSerializationCompatibilityTest {

    @Test
    fun `supabase-kt serializers work with the kotlinx-serialization on the classpath`() {
        val serializer: KSerializer<FileObject> = FileObject.serializer()

        // -- hashing / comparing the descriptor (as Ktor does) calls typeParametersSerializers(),
        // which threw AbstractMethodError on 1.6.3 --
        val descriptor = serializer.descriptor
        assertThat(descriptor.hashCode()).isEqualTo(FileObject.serializer().descriptor.hashCode())
        assertThat(descriptor).isEqualTo(FileObject.serializer().descriptor)
        assertThat(descriptor.elementsCount).isGreaterThan(0)
    }

    @Test
    fun `kotlinx-serialization is at least the version supabase-kt needs`() {
        val version = KSerializer::class.java.`package`.implementationVersion
            ?: return // -- no manifest version (e.g. IDE classpath); the test above still guards --
        val (major, minor) = version.split(".").map { it.takeWhile(Char::isDigit).toInt() }
        assertThat(major * 100 + minor).describedAs("kotlinx-serialization $version").isGreaterThanOrEqualTo(109)
    }
}
