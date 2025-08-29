package com.happyrow.core

import com.happyrow.core.infrastructure.technical.config.SqlDatabaseConfig
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

data class AppConfig(
  val sql: SqlDatabaseConfig,
)

object ConfigLoader {
  private val config: Config = ConfigFactory.load("application.conf")

  fun getConfig(): AppConfig = config.extract<AppConfig>("application")
}
