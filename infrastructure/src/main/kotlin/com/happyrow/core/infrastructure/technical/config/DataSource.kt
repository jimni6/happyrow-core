package com.happyrow.core.infrastructure.technical.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun dataSource(sqlDatabaseConfig: SqlDatabaseConfig): DataSource {
  println("DEBUG: dataSource function called with jdbcUrl: ${sqlDatabaseConfig.jdbcUrl}")
  
  val config = HikariConfig().apply {
    // Parse and convert Render PostgreSQL URL format
    val (jdbcUrl, username, password) = parsePostgreSQLUrl(
      sqlDatabaseConfig.jdbcUrl,
      sqlDatabaseConfig.username,
      sqlDatabaseConfig.password
    )
    
    println("DEBUG: After parsing - jdbcUrl: $jdbcUrl, username: $username")
    
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
  println("DEBUG: parsePostgreSQLUrl called with: $jdbcUrl")
  
  return if (jdbcUrl.startsWith("postgresql://")) {
    println("DEBUG: Parsing Render format URL")
    // Parse Render format: postgresql://user:password@host:port/database
    val urlWithoutPrefix = jdbcUrl.removePrefix("postgresql://")
    val parts = urlWithoutPrefix.split("@")
    
    if (parts.size == 2) {
      val credentials = parts[0].split(":")
      val hostAndDb = parts[1]
      
      if (credentials.size == 2) {
        val parsedUsername = credentials[0]
        val parsedPassword = credentials[1]
        val properJdbcUrl = "jdbc:postgresql://$hostAndDb"
        
        println("DEBUG: Parsed successfully - URL: $properJdbcUrl, User: $parsedUsername")
        Triple(properJdbcUrl, parsedUsername, parsedPassword)
      } else {
        println("DEBUG: Failed to parse credentials, using fallback")
        // Fallback if parsing fails
        Triple("jdbc:$jdbcUrl", username, password)
      }
    } else {
      println("DEBUG: Failed to split URL at @, using fallback")
      // Fallback if parsing fails
      Triple("jdbc:$jdbcUrl", username, password)
    }
  } else if (jdbcUrl.startsWith("jdbc:postgresql://")) {
    println("DEBUG: URL already in correct JDBC format")
    // Already in correct format
    Triple(jdbcUrl, username, password)
  } else {
    println("DEBUG: Adding jdbc: prefix")
    // Add jdbc: prefix
    Triple("jdbc:$jdbcUrl", username, password)
  }
}

fun Application.shutdownDataSource(dataSource: DataSource) {
//  logger.info("Closing $dataSource...")
  (dataSource as HikariDataSource).close()
//  logger.info("Datasource $dataSource closed")
}
