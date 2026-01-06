package com.happyrow.core.infrastructure.technical.auth

import arrow.core.Either
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

private const val JWKS_CACHE_SIZE = 10L
private const val JWKS_CACHE_DURATION_HOURS = 24L
private const val JWKS_RATE_LIMIT_BUCKET_SIZE = 10L
private const val JWKS_RATE_LIMIT_REFILL_RATE_PER_MINUTE = 1L

class SupabaseJwtService(
  private val config: SupabaseJwtConfig,
) {
  private val jwkProvider = JwkProviderBuilder(URL(config.jwksUrl))
    .cached(JWKS_CACHE_SIZE, JWKS_CACHE_DURATION_HOURS, TimeUnit.HOURS)
    .rateLimited(JWKS_RATE_LIMIT_BUCKET_SIZE, JWKS_RATE_LIMIT_REFILL_RATE_PER_MINUTE, TimeUnit.MINUTES)
    .build()

  fun validateToken(token: String): Either<JwtValidationException, AuthenticatedUser> = Either.catch {
    val decodedJWT = decodeAndVerify(token)
    extractUser(decodedJWT)
  }.mapLeft { exception ->
    when (exception) {
      is JWTVerificationException -> JwtValidationException.InvalidToken(exception.message ?: "Invalid token")
      is JwtValidationException -> exception
      else -> JwtValidationException.UnknownError(exception.message ?: "Unknown error")
    }
  }

  private fun decodeAndVerify(token: String): DecodedJWT {
    val jwt = JWT.decode(token)

    val jwk = jwkProvider.get(jwt.keyId)
    val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)

    val verifier = JWT.require(algorithm)
      .withIssuer(config.issuer)
      .withAudience(config.audience)
      .build()

    return verifier.verify(token)
  }

  private fun extractUser(jwt: DecodedJWT): AuthenticatedUser {
    val userId = jwt.subject
      ?: throw JwtValidationException.MissingClaim("sub (user ID)")

    val email = jwt.getClaim("email").asString()
      ?: throw JwtValidationException.MissingClaim("email")

    val role = jwt.getClaim("role").asString() ?: "authenticated"

    return AuthenticatedUser(
      id = userId,
      email = email,
      role = role,
    )
  }
}

data class AuthenticatedUser(
  val id: String,
  val email: String,
  val role: String,
)

sealed class JwtValidationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
  class InvalidToken(message: String) : JwtValidationException(message)
  class MissingClaim(claimName: String) : JwtValidationException("Missing required claim: $claimName")
  class UnknownError(message: String) : JwtValidationException(message)
}
