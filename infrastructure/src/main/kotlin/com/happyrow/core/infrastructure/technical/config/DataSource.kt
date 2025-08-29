package com.happyrow.core.infrastructure.technical.config

//import com.betclic.targeting.audience.configuration.infrastructure.technical.ktor.logger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun dataSource(sqlDatabaseConfig: SqlDatabaseConfig): DataSource {
  val config = HikariConfig().apply {
    this.jdbcUrl = sqlDatabaseConfig.jdbcUrl
    driverClassName = "org.postgresql.Driver"
    this.username = sqlDatabaseConfig.username
    this.password = sqlDatabaseConfig.password
    maximumPoolSize = sqlDatabaseConfig.maximumPoolSize
  }
  val logger = LoggerFactory.getLogger(Application::class.java)
  logger.info("connecting with config url: ${config.jdbcUrl}, username: ${config.username}")
  return HikariDataSource(config)
}

fun Application.shutdownDataSource(dataSource: DataSource) {
//  logger.info("Closing $dataSource...")
  (dataSource as HikariDataSource).close()
//  logger.info("Datasource $dataSource closed")
}
