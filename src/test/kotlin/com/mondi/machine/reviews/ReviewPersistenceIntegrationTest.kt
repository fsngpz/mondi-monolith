package com.mondi.machine.reviews

import com.mondi.machine.TestMondiApplication
import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.accounts.profiles.ProfileRepository
import com.mondi.machine.auths.users.OAuthProvider
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRepository
import com.mondi.machine.configs.AuditorConfig
import com.mondi.machine.products.Product
import com.mondi.machine.products.ProductCategory
import com.mondi.machine.products.ProductRepository
import com.mondi.machine.utils.Currency
import jakarta.persistence.EntityManager
import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.TestPropertySource

/**
 * Runs the migrations on a real PostgreSQL and checks the review table and queries.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMondiApplication::class, AuditorConfig::class)
@TestPropertySource(properties = ["spring.flyway.enabled=true"])
internal class ReviewPersistenceIntegrationTest(
    @Autowired private val reviewRepository: ReviewRepository,
    @Autowired private val productRepository: ProductRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val profileRepository: ProfileRepository,
    @Autowired private val entityManager: EntityManager
) {

    private fun profile(email: String, name: String): Profile {
        val user = userRepository.save(User(email = email, password = "secret", provider = OAuthProvider.LOCAL))
        return profileRepository.save(Profile(user).apply { this.name = name })
    }

    private fun product(sku: String): Product = productRepository.save(
        Product(
            name = "Pearl Earrings $sku",
            description = null,
            currency = Currency.IDR,
            specificationInHtml = null,
            price = BigDecimal("900000"),
            category = ProductCategory.EARRING,
            stock = 5,
            sku = sku
        )
    )

    @Test
    fun `rating summary and product reviews come from the database`() {
        val earrings = product("REVIEW_SKU_A")
        reviewRepository.save(Review(profile("a@example.com", "Ana Putri"), earrings, null, 5, "Lovely"))
        reviewRepository.save(Review(profile("b@example.com", "Budi"), earrings, null, 4, null))
        entityManager.flush()

        // -- execute --
        val summary = reviewRepository.summarizeByProductId(requireNotNull(earrings.id))
        val page = reviewRepository.findAllByProductId(requireNotNull(earrings.id), PageRequest.of(0, 10))

        // -- verify --
        assertThat(summary.average).isEqualTo(4.5)
        assertThat(summary.count).isEqualTo(2)
        assertThat(page.totalElements).isEqualTo(2)
    }

    @Test
    fun `product without reviews has a null average`() {
        val summary = reviewRepository.summarizeByProductId(requireNotNull(product("REVIEW_SKU_B").id))

        assertThat(summary.average).isNull()
        assertThat(summary.count).isZero()
    }

    @Test
    fun `a customer can review a product only once`() {
        val earrings = product("REVIEW_SKU_C")
        val ana = profile("c@example.com", "Ana")
        reviewRepository.saveAndFlush(Review(ana, earrings, null, 5, null))

        // -- execute & verify --
        assertThrows<DataIntegrityViolationException> {
            reviewRepository.saveAndFlush(Review(ana, earrings, null, 1, null))
        }
    }

    @Test
    fun `backoffice search filters by rating and matches product, comment and email`() {
        val earrings = product("REVIEW_SKU_D")
        reviewRepository.save(Review(profile("dewi@example.com", "Dewi"), earrings, null, 5, "Stunning shine"))
        reviewRepository.save(Review(profile("eka@example.com", "Eka"), earrings, null, 2, "Clasp broke"))
        entityManager.flush()
        val pageable = PageRequest.of(0, 10)

        // -- execute & verify --
        assertThat(reviewRepository.findAllCustom(null, null, pageable).totalElements).isEqualTo(2)
        assertThat(reviewRepository.findAllCustom(null, 2, pageable).content.map { it.comment }).containsExactly("Clasp broke")
        assertThat(reviewRepository.findAllCustom("shine", null, pageable).totalElements).isEqualTo(1)
        assertThat(reviewRepository.findAllCustom("DEWI@", null, pageable).totalElements).isEqualTo(1)
        assertThat(reviewRepository.findAllCustom("Pearl Earrings", null, pageable).totalElements).isEqualTo(2)
    }
}
