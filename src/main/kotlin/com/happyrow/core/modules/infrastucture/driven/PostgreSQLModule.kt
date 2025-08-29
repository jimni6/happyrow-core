package com.happyrow.core.modules.infrastucture.driven

import com.happyrow.core.AppConfig
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import com.happyrow.core.infrastructure.technical.config.dataSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val postgresqlModule = module {
  single { get<AppConfig>().sql }
  singleOf(::dataSource)
  singleOf(::ExposedDatabase)
}