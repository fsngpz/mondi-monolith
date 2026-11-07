package com.mondi.machine.auths.users

import com.mondi.machine.TestMondiApplication
import com.mondi.machine.accounts.profiles.ProfileRepository
import com.mondi.machine.notifications.emails.EmailService
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * Checks that registering a user also stores their profile, against a real PostgreSQL
 * (Testcontainers) with the Flyway migrations applied.
 *
 * The profile is created by [com.mondi.machine.accounts.profiles.NewProfileEventListener] after
 * the registration transaction commits. Writes made at that point are lost unless the listener
 * runs in its own transaction, which mocks cannot show.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestMondiApplication::class)
@TestPropertySource(properties = ["spring.flyway.enabled=true"])
internal class UserRegistrationProfileIntegrationTest(
    @Autowired private val userRegistrationService: UserRegistrationService,
    @Autowired private val profileRepository: ProfileRepository,
    @Autowired private val jdbcTemplate: JdbcTemplate
) {
    // -- no real emails --
    @MockitoBean
    lateinit var mockEmailService: EmailService

    @Test
    fun `registering a user stores their profile`() {
        // -- execute --
        val user = userRegistrationService.create("new.customer@example.com", "Secret123!")

        // -- verify --
        val userId = requireNotNull(user.id)
        assertThat(profileRepository.findById(userId)).isPresent
    }

    @Test
    fun `backfill migration creates profiles for users without one`() {
        val sql = javaClass.getResource("/db/migration/R__Backfill_Missing_Profiles.sql")!!.readText()
        // -- one connection on the "mondi" schema, as Flyway runs it --
        val count = jdbcTemplate.execute(ConnectionCallback { connection ->
            connection.schema = "mondi"
            connection.createStatement().use { statement ->
                // -- a user registered while the bug was live: no profile row --
                val userId = statement.executeQuery(
                    "INSERT INTO users (email, password, creator_id, updater_id) " +
                        "VALUES ('orphan@example.com', 'x', 'SYSTEM', 'SYSTEM') RETURNING id"
                ).use { it.next(); it.getLong(1) }

                // -- execute the migration twice: the second run changes nothing --
                statement.execute(sql)
                statement.execute(sql)

                statement.executeQuery("SELECT COUNT(*) FROM profiles WHERE id = $userId")
                    .use { it.next(); it.getLong(1) }
            }
        })

        // -- verify --
        assertThat(count).isEqualTo(1L)
    }
}
