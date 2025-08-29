package com.happyrow.core.infrastructure.technical.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun dataSource(sqlDatabaseConfig: SqlDatabaseConfig): DataSource {
  val config = HikariConfig().apply {
    // Parse and convert Render PostgreSQL URL format
    val (jdbcUrl, username, password) = parsePostgreSQLUrl(
      sqlDatabaseConfig.jdbcUrl,
      sqlDatabaseConfig.username,
      sqlDatabaseConfig.password
    )
    
    this.jdbcUrl = jdbcUrl
    this.username = username
    this.password = password
    driverClassName = "org.postgresql.Driver"
    maximumPoolSize = sqlDatabaseConfig.maximumPoolSize
    
    // SSL configuration for Render PostgreSQL
    addDataSourceProperty("sslmode", "require")
    
    // Connection validation
    isAutoCommit = false
    validate()
  }
  val logger = LoggerFactory.getLogger(Application::class.java)
  logger.info("connecting with config url: ${config.jdbcUrl}, username: ${config.username}")
  return HikariDataSource(config)
}

private fun parsePostgreSQLUrl(jdbcUrl: String, username: String, password: String): Triple<String, String, String> {
  return if (jdbcUrl.startsWith("jdbc:postgresql://") && jdbcUrl.contains("@")) {
    // Handle Render format: jdbc:postgresql://user:password@host:port/database
    val urlWithoutPrefix = jdbcUrl.removePrefix("jdbc:postgresql://")
    val parts = urlWithoutPrefix.split("@")
    
    if (parts.size == 2) {
      val credentials = parts[0].split(":")
      val hostAndDb = parts[1]
      
      if (credentials.size == 2) {
        val parsedUsername = credentials[0]
        val parsedPassword = credentials[1]
        val cleanJdbcUrl = "jdbc:postgresql://$hostAndDb"
        
        Triple(cleanJdbcUrl, parsedUsername, parsedPassword)
      } else {
        // Fallback if parsing fails
        Triple(jdbcUrl, username, password)
      }
    } else {
      // Fallback if parsing fails
      Triple(jdbcUrl, username, password)
    }
  } else {
    // URL is already in correct format or use provided credentials
    Triple(jdbcUrl, username, password)
  }
}

fun Application.shutdownDataSource(dataSource: DataSource) {
//  logger.info("Closing $dataSource...")
  (dataSource as HikariDataSource).close()
//  logger.info("Datasource $dataSource closed")
}
