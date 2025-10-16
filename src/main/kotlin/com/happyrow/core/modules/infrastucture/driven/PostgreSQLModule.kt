package com.happyrow.core.modules.infrastucture.driven

import com.happyrow.core.AppConfig
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.infrastructure.common.driven.event.SqlEventRepository
import com.happyrow.core.infrastructure.participant.common.driven.SqlParticipantRepository
import com.happyrow.core.infrastructure.technical.config.DatabaseInitializer
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import com.happyrow.core.infrastructure.technical.config.dataSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.sql.SQLException

val postgresqlModule = module {
  single {
    println("DEBUG: Creating SqlDatabaseConfig from AppConfig")
    try {
      val appConfig = get<AppConfig>()
      println("DEBUG: AppConfig retrieved: $appConfig")
      val sqlConfig = appConfig.sql
      println("DEBUG: SqlDatabaseConfig created: $sqlConfig")
      sqlConfig
    } catch (e: SQLException) {
      println("DEBUG: Failed to create SqlDatabaseConfig: ${e.message}")
      e.printStackTrace()
      throw e
    }
  }

  single {
    println("DEBUG: Creating DataSource")
    try {
      val sqlConfig = get<com.happyrow.core.infrastructure.technical.config.SqlDatabaseConfig>()
      println("DEBUG: SqlDatabaseConfig for DataSource: $sqlConfig")
      val ds = dataSource(sqlConfig)
      println("DEBUG: DataSource created successfully: $ds")
      ds
    } catch (e: SQLException) {
      println("DEBUG: Failed to create DataSource: ${e.message}")
      e.printStackTrace()
      throw e
    }
  }

  singleOf(::ExposedDatabase)
  singleOf(::DatabaseInitializer)
  singleOf(::SqlEventRepository) bind EventRepository::class
  singleOf(::SqlParticipantRepository) bind ParticipantRepository::class
}
