package com.happyrow.core.infrastructure.technical.auth

data class SupabaseJwtConfig(
  val jwksUrl: String,
  val issuer: String,
  val audience: String,
) {
  companion object {
    fun fromEnvironment(): SupabaseJwtConfig {
      val supabaseUrl = System.getenv("SUPABASE_URL")
        ?: throw IllegalStateException("SUPABASE_URL environment variable is required")
      
      val supabaseProjectId = extractProjectId(supabaseUrl)
      
      return SupabaseJwtConfig(
        jwksUrl = "$supabaseUrl/auth/v1/jwks",
        issuer = "$supabaseUrl/auth/v1",
        audience = "authenticated",
      )
    }
    
    private fun extractProjectId(url: String): String {
      val regex = Regex("https://([a-z0-9]+)\\.supabase\\.co")
      return regex.find(url)?.groupValues?.get(1)
        ?: throw IllegalStateException("Invalid Supabase URL format: $url")
    }
  }
}
