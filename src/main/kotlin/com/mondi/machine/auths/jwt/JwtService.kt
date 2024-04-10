package com.mondi.machine.auths.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import java.security.Key
import java.util.Date
import java.util.function.Function
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

/**
 * The component service class to handle all Jwt logic.
 *
 * @author Ferdinand Sangap
 * @since 2023-05-27
 */
@Component
class JwtService {
  fun extractEmail(token: String?): String {
    return extractClaim(token, Claims::getSubject)
  }

  fun extractExpiration(token: String?): Date {
    return extractClaim(token, Claims::getExpiration)
  }

  fun <T> extractClaim(token: String?, claimsResolver: Function<Claims, T>): T {
    val claims: Claims = extractAllClaims(token)
    return claimsResolver.apply(claims)
  }

  private fun extractAllClaims(token: String?): Claims {
    return Jwts
      .parserBuilder()
      .setSigningKey(getSignKey())
      .build()
      .parseClaimsJws(token)
      .body
  }

  private fun isTokenExpired(token: String?): Boolean {
    return extractExpiration(token).before(Date())
  }

  fun validateToken(token: String?, userDetails: UserDetails): Boolean {
    val username = extractEmail(token)
    return (username == userDetails.username && !isTokenExpired(token))
  }

  fun generateToken(email: String): String {
    val claims: Map<String, Any> = HashMap()
    return createToken(claims, email)
  }

  private fun createToken(claims: Map<String, Any>, email: String): String {
    return Jwts.builder()
      .setClaims(claims)
      .setSubject(email)
      .setIssuedAt(Date(System.currentTimeMillis()))
      .setExpiration(Date(System.currentTimeMillis() + THIRTY_MIN))
      .signWith(getSignKey(), SignatureAlgorithm.HS256).compact()
  }

  private fun getSignKey(): Key {
    val keyBytes: ByteArray = Decoders.BASE64.decode(SECRET)
    return Keys.hmacShaKeyFor(keyBytes)
  }

  companion object {
    const val SECRET: String = "413F4428472B4B6250655368566D5970337336763979244226452948404D6351"
    const val THIRTY_MIN = 1000 * 60 * 30
  }
}