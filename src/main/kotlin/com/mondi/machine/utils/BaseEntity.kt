package com.mondi.machine.utils

import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.Hibernate
import org.springframework.data.domain.Persistable

/**
 * This abstract class represents a base entity with a primary key of type Long.
 * It implements the Persistable interface and provides a basic implementation for id, version, equals, and hashCode.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-08
 */
@MappedSuperclass
@EntityListeners
abstract class BaseEntity : Persistable<Long?> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @get:JvmName(name = "id")
  var id: Long? = null

  @Version
  val version: Long = 0

  /**
   * Returns the id of the entity.
   *
   * @return the id. Can be `null`.
   */
  override fun getId(): Long? {
    return id
  }

  /**
   * Returns if the [Persistable] is new or was persisted already.
   *
   * @return if `true` the object is new.
   */
  override fun isNew(): Boolean {
    return id == null
  }

  /**
   * JPA entity object/instance equality check based on the `id` primary key.
   * i.e. 2 instances that both have not been stored in the database will never be equal.
   *
   * Reference: [How to implement equals and hashCode using the JPA entity identifier (Primary Key)](https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/)
   *
   * @author Vlad Mihalcea
   * @author Ferdinand Sangap
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if ((other == null) || (this.javaClass != other.javaClass)) return false
    if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as BaseEntity

    return (this.id != null) && (this.id == other.id)
  }

  /**
   * JPA entity object/instance static hashcode.
   * i.e. the hashcode value should not change between the time the object is created and the time
   * it is persisted in the database.
   */
  override fun hashCode(): Int {
    return this.javaClass.hashCode()
  }
}