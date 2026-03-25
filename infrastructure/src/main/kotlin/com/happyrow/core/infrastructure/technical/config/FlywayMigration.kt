package com.happyrow.core.infrastructure.technical.config

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class FlywayMigration(private val dataSource: DataSource) {
  private val logger = LoggerFactory.getLogger(FlywayMigration::class.java)

  @Suppress("TooGenericExceptionCaught")
  fun migrate() {
    logger.info("Running Flyway database migrations...")
    // #region agent log — H1-H5: capture flyway state and error details
    try {
      val flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load()

      val info = flyway.info()
      logger.error(
        "[DEBUG-0ccd04] Flyway pending migrations: {}",
        info.pending().map {
          it.version.toString() + ":" + it.description
        },
      )
      logger.error(
        "[DEBUG-0ccd04] Flyway applied migrations: {}",
        info.applied().map {
          it.version.toString() + ":" + it.description + ":" + it.state
        },
      )
      logger.error("[DEBUG-0ccd04] Flyway current version: {}", info.current()?.version)
      logger.error(
        "[DEBUG-0ccd04] Flyway resolved migrations: {}",
        info.all().map {
          it.version.toString() + ":" + it.description + ":" + it.state + ":" + it.type
        },
      )

      val result = flyway.migrate()
      logger.info("Flyway migrations completed: {} migrations applied", result.migrationsExecuted)
    } catch (e: Exception) {
      logger.error("[DEBUG-0ccd04] Flyway migration FAILED — exception class: {}", e.javaClass.name)
      logger.error("[DEBUG-0ccd04] Flyway migration FAILED — message: {}", e.message)
      logger.error("[DEBUG-0ccd04] Flyway migration FAILED — cause: {}", e.cause?.message)
      logger.error(
        "[DEBUG-0ccd04] Flyway migration FAILED — root cause: {}",
        generateSequence(e as Throwable) {
          it.cause
        }.last().message,
      )
      logger.error("[DEBUG-0ccd04] Flyway migration FAILED — full stack", e)
      throw e
    }
    // #endregion
  }
}
