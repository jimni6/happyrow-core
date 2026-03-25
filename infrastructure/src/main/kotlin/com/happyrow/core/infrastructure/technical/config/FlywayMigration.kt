package com.happyrow.core.infrastructure.technical.config

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class FlywayMigration(private val dataSource: DataSource) {
  private val logger = LoggerFactory.getLogger(FlywayMigration::class.java)

  fun migrate() {
    logger.info("Running Flyway database migrations...")
    val result = Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .baselineOnMigrate(true)
      .load()
      .migrate()
    logger.info("Flyway migrations completed: {} migrations applied", result.migrationsExecuted)
  }
}
