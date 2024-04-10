package com.mondi.machine.utils

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import java.time.OffsetDateTime
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * The class that contains the auditing field/column such as createdAt, creatorId, updatedAt and
 * updaterId. This class also implements the [BaseEntity].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AuditableBaseEntity<T> (
  @CreatedBy
  var creatorId: T? = null,
  @CreationTimestamp
  var createdAt: OffsetDateTime? = null,

  @LastModifiedBy
  var updaterId: T? = null,
  @UpdateTimestamp
  var updatedAt: OffsetDateTime? = null
): BaseEntity()