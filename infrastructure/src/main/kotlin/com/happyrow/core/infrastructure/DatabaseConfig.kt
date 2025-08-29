package com.happyrow.core.infrastructure

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

data class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
    val driver: String = "org.postgresql.Driver",
    val maxPoolSize: Int = 10,
    val connectionTimeout: Long = 30000,
    val idleTimeout: Long = 600000,
    val maxLifetime: Long = 1800000,
    val sslMode: String = "require"
)

object DatabaseFactory {

    fun init(config: DatabaseConfig): Database {
        val dataSource = createDataSource(config)
        return Database.connect(dataSource)
    }

    private fun createDataSource(config: DatabaseConfig): DataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.username
            password = config.password
            driverClassName = config.driver
            maximumPoolSize = config.maxPoolSize
            connectionTimeout = config.connectionTimeout
            idleTimeout = config.idleTimeout
            maxLifetime = config.maxLifetime

            // SSL configuration for Render PostgreSQL
            addDataSourceProperty("sslmode", config.sslMode)
            addDataSourceProperty("sslrootcert", "server-ca.pem")
            addDataSourceProperty("sslcert", "client-cert.pem")
            addDataSourceProperty("sslkey", "client-key.pem")

            // Connection validation
            isAutoCommit = false
            validate()
        }

        return HikariDataSource(hikariConfig)
    }

    fun createTables(vararg tables: org.jetbrains.exposed.sql.Table) {
        transaction {
            SchemaUtils.create(*tables)
        }
    }
}
