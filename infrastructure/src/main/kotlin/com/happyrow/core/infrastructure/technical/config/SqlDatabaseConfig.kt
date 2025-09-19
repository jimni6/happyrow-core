package com.happyrow.core.infrastructure.technical.config

data class SqlDatabaseConfig(
  val jdbcUrl: String,
  val username: String,
  val password: String,
  val maximumPoolSize: Int = 50,
)
