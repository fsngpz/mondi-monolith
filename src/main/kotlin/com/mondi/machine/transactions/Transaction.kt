package com.mondi.machine.transactions

import com.mondi.machine.accounts.profiles.Profile
import com.mondi.machine.utils.AuditableBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The entity class for Transaction.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-14
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "transactions")
class Transaction(
  var productName: String,
  var price: BigDecimal,
  var certificateUrl: String,
  var purchasedAt: OffsetDateTime,

  // -- Many to One --
  @ManyToOne
  @JoinColumn(name = "profile_id")
  val profile: Profile
) : AuditableBaseEntity<String>()