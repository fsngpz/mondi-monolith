import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springDocVersion = "2.7.0"
val ktorVersion = "3.3.3"
val supabaseVersion = "3.3.0"
val mockitoKotlin = "5.2.1"
val jwtVersion = "0.11.5"
val dropboxVersion = "6.1.0"
val coroutinesVersion = "1.10.2"
val mockkVersion = "1.13.14"
val apacheCommonsIoVersion = "2.21.0"
val googleApiClientVersion = "2.7.2"
val owaspSanitizerVersion = "20260102.1"

plugins {
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
}

group = "com.mondi"
version = "machine-0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}



dependencies {
    // -- spring boot --
    implementation("org.springframework.boot:spring-boot-starter-web")

    // -- spring security --
    implementation("org.springframework.boot:spring-boot-starter-security")

    // -- spring boot: data --
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // -- flyway --
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // -- apache commons io --
    implementation("commons-io:commons-io:$apacheCommonsIoVersion")

    // -- OWASP Java HTML Sanitizer for XSS prevention --
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:$owaspSanitizerVersion")

    // -- spring doc --
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

    // -- test --
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${mockitoKotlin}")

    // -- kotlin --
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // -- jwt --
    implementation("io.jsonwebtoken:jjwt-api:${jwtVersion}")
    implementation("io.jsonwebtoken:jjwt-impl:${jwtVersion}")
    implementation("io.jsonwebtoken:jjwt-jackson:${jwtVersion}")

    // -- google oauth2 --
    implementation("com.google.api-client:google-api-client:${googleApiClientVersion}")

    // -- dropbox --
    implementation("com.dropbox.core:dropbox-core-sdk:${dropboxVersion}")

    // -- supabase--
    implementation("io.github.jan-tennert.supabase:supabase-kt:${supabaseVersion}")
    implementation("io.github.jan-tennert.supabase:storage-kt:${supabaseVersion}")

    // -- ktor --
    implementation("io.ktor:ktor-client-java:${ktorVersion}")

    // -- coroutine --
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${coroutinesVersion}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${coroutinesVersion}")

    // -- mockk --
    testImplementation("io.mockk:mockk:${mockkVersion}")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-java-parameters",
            "-Xannotation-default-target=param-property"
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
