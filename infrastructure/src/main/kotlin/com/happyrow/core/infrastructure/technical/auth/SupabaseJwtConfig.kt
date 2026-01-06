package com.happyrow.core.infrastructure.technical.auth

data class SupabaseJwtConfig(
  val jwksUrl: String,
  val issuer: String,
  val audience: String,
) {
  companion object {
    fun fromEnvironment(): SupabaseJwtConfig {
      val supabaseUrl = System.getenv("SUPABASE_URL")
        ?: error("SUPABASE_URL environment variable is required")

      return SupabaseJwtConfig(
        jwksUrl = "$supabaseUrl/auth/v1/jwks",
        issuer = "$supabaseUrl/auth/v1",
        audience = "authenticated",
      )
    }
  }
}
