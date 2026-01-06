package com.happyrow.core.modules.infrastucture

import com.happyrow.core.infrastructure.technical.auth.SupabaseJwtConfig
import com.happyrow.core.infrastructure.technical.auth.SupabaseJwtService
import org.koin.dsl.module

val authModule = module {
  single { SupabaseJwtConfig.fromEnvironment() }
  single { SupabaseJwtService(get()) }
}
