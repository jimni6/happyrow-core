package com.happyrow.core.infrastructure.technical.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey

private const val BEARER_PREFIX_LENGTH = 7

val SupabaseAuthKey = AttributeKey<AuthenticatedUser>("SupabaseAuth")

val JwtAuthenticationPlugin = createApplicationPlugin(
  name = "JwtAuthenticationPlugin",
  createConfiguration = ::JwtAuthConfig,
) {
  val jwtService = pluginConfig.jwtService

  onCall { call ->
    val token = extractBearerToken(call)

    if (token == null) {
      call.respond(
        HttpStatusCode.Unauthorized,
        mapOf(
          "type" to "MISSING_TOKEN",
          "message" to "Authorization header with Bearer token is required",
        ),
      )
      return@onCall
    }

    jwtService.validateToken(token).fold(
      ifLeft = { exception ->
        call.respond(
          HttpStatusCode.Unauthorized,
          mapOf(
            "type" to "INVALID_TOKEN",
            "message" to exception.message,
          ),
        )
      },
      ifRight = { user ->
        call.attributes.put(SupabaseAuthKey, user)
      },
    )
  }
}

class JwtAuthConfig {
  lateinit var jwtService: SupabaseJwtService
}

private fun extractBearerToken(call: ApplicationCall): String? {
  val authHeader = call.request.headers["Authorization"]

  return when {
    authHeader == null -> null
    !authHeader.startsWith("Bearer ", ignoreCase = true) -> null
    else -> authHeader.substring(BEARER_PREFIX_LENGTH).trim()
  }
}

fun ApplicationCall.authenticatedUser(): AuthenticatedUser {
  return attributes.getOrNull(SupabaseAuthKey)
    ?: error("User not authenticated. JwtAuthenticationPlugin must be installed.")
}
