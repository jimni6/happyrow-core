package com.happyrow.core.infrastructure.technical.auth

data class SupabaseJwtConfig(
  val jwtSecret: String,
  val issuer: String,
  val audience: String,
) {
  companion object {
    fun fromEnvironment(): SupabaseJwtConfig {
      val supabaseUrl = System.getenv("SUPABASE_URL")
        ?: error("SUPABASE_URL environment variable is required")
      val jwtSecret = System.getenv("SUPABASE_JWT_SECRET")
        ?: error("SUPABASE_JWT_SECRET environment variable is required")

      return SupabaseJwtConfig(
        jwtSecret = jwtSecret,
        issuer = "$supabaseUrl/auth/v1",
        audience = "authenticated",
      )
    }
  }
}
