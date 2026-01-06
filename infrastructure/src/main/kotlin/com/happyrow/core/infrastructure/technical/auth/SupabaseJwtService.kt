package com.happyrow.core.infrastructure.technical.auth

import arrow.core.Either
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT

class SupabaseJwtService(
  private val config: SupabaseJwtConfig,
) {
  private val algorithm = Algorithm.HMAC256(config.jwtSecret)

  fun validateToken(token: String): Either<Throwable, AuthenticatedUser> {
    return Either.catch {
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
