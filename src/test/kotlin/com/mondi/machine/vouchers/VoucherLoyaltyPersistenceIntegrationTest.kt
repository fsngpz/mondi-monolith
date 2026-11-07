package com.mondi.machine.vouchers

import com.mondi.machine.TestMondiApplication
import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.accounts.profiles.ProfileRepository
import com.mondi.machine.auths.users.OAuthProvider
import com.mondi.machine.auths.users.User
import com.mondi.machine.auths.users.UserRepository
import com.mondi.machine.configs.AuditorConfig
import com.mondi.machine.loyalty.LoyaltyPoint
import com.mondi.machine.loyalty.LoyaltyPointRepository
import com.mondi.machine.loyalty.LoyaltyTransactionType
import jakarta.persistence.EntityManager
import java.math.BigDecimal
import java.time.OffsetDateTime
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
 * Runs the migrations on a real PostgreSQL and checks the voucher and loyalty tables and queries.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-08
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMondiApplication::class, AuditorConfig::class)
@TestPropertySource(properties = ["spring.flyway.enabled=true"])
internal class VoucherLoyaltyPersistenceIntegrationTest(
    @Autowired private val voucherRepository: VoucherRepository,
    @Autowired private val loyaltyPointRepository: LoyaltyPointRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val profileRepository: ProfileRepository,
    @Autowired private val entityManager: EntityManager
) {

    private fun voucher(code: String, maxUsage: Int?) = voucherRepository.saveAndFlush(
        Voucher(
            code = code,
            description = "Test voucher",
            type = VoucherType.PERCENTAGE,
            value = BigDecimal("10"),
            maxDiscountAmount = null,
            maxUsage = maxUsage,
            startDate = OffsetDateTime.now().minusDays(1),
            endDate = OffsetDateTime.now().plusDays(1)
        )
    )

    private fun profile(): Profile {
        val user = userRepository.save(User(email = "points@example.com", password = "secret", provider = OAuthProvider.LOCAL))
        return profileRepository.save(Profile(user))
    }

    @Test
    fun `usage stops at max usage`() {
        val id = requireNotNull(voucher("TWICE", maxUsage = 2).id)

        // -- execute & verify --
        assertThat(voucherRepository.incrementUsageIfAvailable(id)).isEqualTo(1)
        assertThat(voucherRepository.incrementUsageIfAvailable(id)).isEqualTo(1)
        assertThat(voucherRepository.incrementUsageIfAvailable(id)).isEqualTo(0)
        entityManager.clear()
        assertThat(voucherRepository.findById(id).orElseThrow().currentUsage).isEqualTo(2)

        // -- a released use can be taken again --
        assertThat(voucherRepository.decrementUsage(id)).isEqualTo(1)
        assertThat(voucherRepository.incrementUsageIfAvailable(id)).isEqualTo(1)
    }

    @Test
    fun `unlimited vouchers keep counting`() {
        val id = requireNotNull(voucher("UNLIMITED", maxUsage = null).id)

        repeat(3) { assertThat(voucherRepository.incrementUsageIfAvailable(id)).isEqualTo(1) }
    }

    @Test
    fun `codes are unique and found ignoring case`() {
        voucher("WELCOME10", maxUsage = null)

        // -- execute & verify --
        assertThat(voucherRepository.findByCodeIgnoreCase("welcome10")?.code).isEqualTo("WELCOME10")
        assertThat(voucherRepository.existsByCodeIgnoreCase("Welcome10")).isTrue()
        assertThrows<DataIntegrityViolationException> { voucher("welcome10", maxUsage = null) }
    }

    @Test
    fun `backoffice search filters by active flag and code`() {
        voucher("ACTIVE1", null)
        voucherRepository.saveAndFlush(voucher("INACTIVE1", null).apply { isActive = false })
        val pageable = PageRequest.of(0, 10)

        // -- execute & verify --
        assertThat(voucherRepository.findAllCustom(null, null, pageable).totalElements).isEqualTo(2)
        assertThat(voucherRepository.findAllCustom(null, false, pageable).content.map { it.code }).containsExactly("INACTIVE1")
        assertThat(voucherRepository.findAllCustom("active1", true, pageable).content.map { it.code }).containsExactly("ACTIVE1")
    }

    @Test
    fun `points balance sums the ledger`() {
        val profile = profile()
        loyaltyPointRepository.save(LoyaltyPoint(profile, 260, LoyaltyTransactionType.EARNED, null, "Order A"))
        loyaltyPointRepository.save(LoyaltyPoint(profile, -60, LoyaltyTransactionType.ADJUSTMENT, null, "Correction"))
        entityManager.flush()
        val profileId = requireNotNull(profile.id)

        // -- verify the sums --
        assertThat(loyaltyPointRepository.sumPointsByProfileId(profileId)).isEqualTo(200)
        assertThat(loyaltyPointRepository.sumPointsByProfileIdAndType(profileId, LoyaltyTransactionType.EARNED)).isEqualTo(260)
        assertThat(loyaltyPointRepository.sumPointsByProfileId(999_999L)).isZero()
        assertThat(loyaltyPointRepository.findAllByProfileId(profileId, PageRequest.of(0, 10)).totalElements).isEqualTo(2)
    }
}
