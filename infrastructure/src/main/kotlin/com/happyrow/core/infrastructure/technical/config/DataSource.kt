package com.happyrow.core.infrastructure.technical.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory
import java.sql.SQLException
import javax.sql.DataSource

private const val DEFAULT_CONNECTION_TIMEOUT = 30000L
private const val DEFAULT_IDLE_TIMEOUT = 600000L
private const val DEFAULT_MAX_LIFETIME = 1800000L

fun dataSource(sqlDatabaseConfig: SqlDatabaseConfig): DataSource {
  val config = HikariConfig().apply {
    // Parse and convert Render PostgreSQL URL format
    val (jdbcUrl, username, password) = parsePostgreSQLUrl(
      sqlDatabaseConfig.jdbcUrl,
      sqlDatabaseConfig.username,
      sqlDatabaseConfig.password,
    )

    this.jdbcUrl = jdbcUrl
    this.username = username
    this.password = password
    driverClassName = "org.postgresql.Driver"
    maximumPoolSize = sqlDatabaseConfig.maximumPoolSize

    // SSL configuration for Render PostgreSQL
    addDataSourceProperty("sslmode", "require")
    addDataSourceProperty("sslrootcert", "")
    addDataSourceProperty("sslcert", "")
    addDataSourceProperty("sslkey", "")

    // Additional connection properties for Render
    connectionTimeout = DEFAULT_CONNECTION_TIMEOUT
    idleTimeout = DEFAULT_IDLE_TIMEOUT
    maxLifetime = DEFAULT_MAX_LIFETIME

    // Connection validation
    isAutoCommit = false
    validate()
  }
  val logger = LoggerFactory.getLogger(Application::class.java)
  logger.info("Attempting to connect to database:")
  logger.info("  JDBC URL: ${config.jdbcUrl}")
  logger.info("  Username: ${config.username}")
  logger.info("  SSL Mode: require")
  logger.info("  Driver: ${config.driverClassName}")
  logger.info("  Max Pool Size: ${config.maximumPoolSize}")

  return try {
    HikariDataSource(config)
  } catch (e: SQLException) {
    logger.error("Failed to create database connection due to SQL error:", e)
    logger.error("Connection details - URL: ${config.jdbcUrl}, Username: ${config.username}")
    throw e
  } catch (e: HikariPool.PoolInitializationException) {
    logger.error("Failed to initialize connection pool:", e)
    logger.error("Connection details - URL: ${config.jdbcUrl}, Username: ${config.username}")
    throw e
  }
}

private fun parsePostgreSQLUrl(jdbcUrl: String, username: String, password: String): Triple<String, String, String> {
  return when {
    // Handle Render format: postgresql://user:password@host:port/database (without jdbc: prefix)
    jdbcUrl.startsWith("postgresql://") && jdbcUrl.contains("@") -> {
      val urlWithoutPrefix = jdbcUrl.removePrefix("postgresql://")
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
          // Fallback: add jdbc: prefix and use provided credentials
          Triple("jdbc:$jdbcUrl", username, password)
        }
      } else {
        // Fallback: add jdbc: prefix and use provided credentials
        Triple("jdbc:$jdbcUrl", username, password)
      }
    }
    // Handle JDBC format: jdbc:postgresql://user:password@host:port/database
    jdbcUrl.startsWith("jdbc:postgresql://") && jdbcUrl.contains("@") -> {
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
    }
    // Handle simple postgresql:// format without embedded credentials
    jdbcUrl.startsWith("postgresql://") -> {
      Triple("jdbc:$jdbcUrl", username, password)
    }
    // URL is already in correct JDBC format
    else -> {
      Triple(jdbcUrl, username, password)
    }
  }
}

fun Application.shutdownDataSource(dataSource: DataSource) {
//  logger.info("Closing $dataSource...")
  (dataSource as HikariDataSource).close()
//  logger.info("Datasource $dataSource closed")
}
