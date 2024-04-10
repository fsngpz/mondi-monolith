package com.mondi.machine.auths.users

import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository

/**
 * The interface for repository/database [User].
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
interface UserRepository : JpaRepository<User, Long> {

  fun findByEmail(email: String): Optional<User>
}