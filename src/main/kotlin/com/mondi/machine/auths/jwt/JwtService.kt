package com.mondi.machine.auths.jwt

import com.mondi.machine.configs.CustomUserDetails
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import java.security.Key
import java.util.Date
import java.util.function.Function
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

/**
 * The component service class to handle all Jwt logic.
 *
 * @author Ferdinand Sangap
 * @since 2023-05-27
 */
@Component
class JwtService(@Value("\${spring-auth.secret}") private val authSecret: String) {

  /**
   * a function to do extraction of id from JWT Token.
   *
   * @param token the given JWT Token.
   * @return the ID that extracted from JWT.
   */
  fun extractId(token: String?): String {
    return extractClaim(token, Claims::getId)
  }

  /**
   * a function to do extraction of email from JWT Token.
   *
   * @param token the given JWT Token.
   * @return the ID that extracted from JWT.
   */
  fun extractEmail(token: String?): String {
    return extractClaim(token, Claims::getSubject)
  }

  /**
   * a function to do extraction of expiration date from JWT Token.
   *
   * @param token the given JWT Token.
   * @return the ID that extracted from JWT.
   */
  fun extractExpiration(token: String?): Date {
    return extractClaim(token, Claims::getExpiration)
  }

  /**
   * a generic function to extract claim from JWT Token.
   *
   * @param T the Generic Type.
   * @param token  the given JWT Token.
   * @param claimsResolver the Claim Resolver.
   * @return the Generic Type.
   */
  fun <T> extractClaim(token: String?, claimsResolver: Function<Claims, T>): T {
    val claims: Claims = extractAllClaims(token)
    return claimsResolver.apply(claims)
  }

  /**
   * a private function to extract all claims from token.
   *
   * @param token the given JWT token.
   * @return the [Claims].
   */
  private fun extractAllClaims(token: String?): Claims {
    return Jwts
      .parserBuilder()
      .setSigningKey(getSignKey())
      .build()
      .parseClaimsJws(token)
      .body
  }

  /**
   * a function to check is the JWT Token expired.
   *
   * @param token the JWT token.
   * @return the boolean true or false.
   */
  private fun isTokenExpired(token: String?): Boolean {
    return extractExpiration(token).before(Date())
  }

  /**
   * a function to do validation of token.
   *
   * @param token the JWT Token.
   * @param userDetails the [UserDetails].
   * @return the boolean of true or false.
   */
  fun validateToken(token: String?, userDetails: UserDetails): Boolean {
    val username = extractEmail(token)
    return (username == userDetails.username && !isTokenExpired(token))
  }

  /**
   * a function to generate the JWT token.
   *
   * @param userDetails the [CustomUserDetails] instance.
   * @return the generated token.
   */
  fun generateToken(userDetails: CustomUserDetails): String {
    // -- get the email --
    val email = userDetails.username
    // -- get the id --
    val id = userDetails.getId()
    // -- setup new HashMap for claims --
    val claims: Map<String, Any> = HashMap()
    // -- create the JTW Token --
    return createToken(claims, email, id)
  }

  /**
   * a private function to create the token.
   *
   * @param claims the claims in [Map].
   * @param email the email of user.
   * @param id the unique identifier of user.
   * @return the JWT Token expired in 30 minutes.
   */
  private fun createToken(claims: Map<String, Any>, email: String, id: Long): String {
    return Jwts.builder()
      .setClaims(claims)
      .setSubject(email)
      .setId(id.toString())
      .setIssuedAt(Date(System.currentTimeMillis()))
      .setExpiration(Date(System.currentTimeMillis() + THIRTY_MIN))
      .signWith(getSignKey(), SignatureAlgorithm.HS256).compact()
  }

  /**
   * a function to get sign key.
   *
   * @return the [Key] of encryption.
   */
  private fun getSignKey(): Key {
    val keyBytes: ByteArray = Decoders.BASE64.decode(authSecret)
    return Keys.hmacShaKeyFor(keyBytes)
  }

  companion object {
    const val THIRTY_MIN = 1000 * 60 * 30
  }
}