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

  fun validateToken(token: String): Either<Throwable, AuthenticatedUser> {
    return Either.catch {
      val jwt = JWT.decode(token)
      val jwk = jwkProvider.get(jwt.keyId)
      val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)

      val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

      val verifiedJwt = verifier.verify(token)
      extractUser(verifiedJwt)
    }
  }

  private fun extractUser(jwt: DecodedJWT): AuthenticatedUser {
    val userId = jwt.subject
      ?: throw JWTVerificationException("Token missing 'sub' claim")

    val email = jwt.getClaim("email").asString()
      ?: throw JWTVerificationException("Token missing 'email' claim")

    return AuthenticatedUser(
      userId = userId,
      email = email,
    )
  }
}

data class AuthenticatedUser(
  val userId: String,
  val email: String,
)
