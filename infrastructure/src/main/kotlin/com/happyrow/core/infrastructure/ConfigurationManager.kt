package com.happyrow.core.infrastructure

import io.ktor.server.config.ApplicationConfig

object ConfigurationManager {

    fun getDatabaseConfig(config: ApplicationConfig): DatabaseConfig {
        // Try to get from environment variables first (Render deployment)
        val databaseUrl = System.getenv("DATABASE_URL")

        return if (databaseUrl != null) {
            // Parse Render PostgreSQL URL format: postgresql://user:password@host:port/database
            parseRenderDatabaseUrl(databaseUrl)
        } else {
            // Fallback to application.conf configuration
            DatabaseConfig(
                url = config.property("database.url").getString(),
                username = config.property("database.username").getString(),
                password = config.property("database.password").getString(),
                maxPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toInt() ?: 10,
                connectionTimeout = config.propertyOrNull("database.connectionTimeout")?.getString()?.toLong() ?: 30000,
                idleTimeout = config.propertyOrNull("database.idleTimeout")?.getString()?.toLong() ?: 600000,
                maxLifetime = config.propertyOrNull("database.maxLifetime")?.getString()?.toLong() ?: 1800000,
                sslMode = config.propertyOrNull("database.sslMode")?.getString() ?: "require"
            )
        }
    }

    private fun parseRenderDatabaseUrl(databaseUrl: String): DatabaseConfig {
        // Remove postgresql:// prefix
        val urlWithoutPrefix = databaseUrl.removePrefix("postgresql://")

        // Split into credentials and connection parts
        val parts = urlWithoutPrefix.split("@")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid DATABASE_URL format")
        }

        val credentials = parts[0].split(":")
        if (credentials.size != 2) {
            throw IllegalArgumentException("Invalid DATABASE_URL credentials format")
        }

        val username = credentials[0]
        val password = credentials[1]

        val connectionParts = parts[1].split("/")
        if (connectionParts.size != 2) {
            throw IllegalArgumentException("Invalid DATABASE_URL connection format")
        }

        val hostPort = connectionParts[0]
        val database = connectionParts[1]

        // Reconstruct the JDBC URL
        val jdbcUrl = "jdbc:postgresql://$hostPort/$database"

        return DatabaseConfig(
            url = jdbcUrl,
            username = username,
            password = password,
            sslMode = "require" // Render requires SSL
        )
    }
}