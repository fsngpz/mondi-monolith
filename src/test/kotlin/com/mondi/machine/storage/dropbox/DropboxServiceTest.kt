package com.mondi.machine.storage.dropbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * The test class for [DropboxService].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-11
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DropboxServiceTest(@Autowired private val dropboxService: DropboxService) {
    // -- region of smoke testing --
    @Test
    fun `dependencies are not null`() {
        assertThat(dropboxService).isNotNull
    }
    // -- end of region smoke testing --
}
