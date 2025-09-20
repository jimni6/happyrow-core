package com.happyrow.core.infrastructure.technical.config

import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class DatabaseInitializer(
  private val exposedDatabase: ExposedDatabase,
) {
  private val logger = LoggerFactory.getLogger(DatabaseInitializer::class.java)

  fun initializeDatabase() {
    logger.info("Starting database initialization for Render PostgreSQL...")

    transaction(exposedDatabase.database) {
      // Create schema if it doesn't exist
      logger.info("Creating configuration schema...")
      exec("CREATE SCHEMA IF NOT EXISTS configuration")

      // Create EVENT_TYPE enum if it doesn't exist
      logger.info("Creating EVENT_TYPE enum...")
      exec(
        """
        DO $$ BEGIN
          CREATE TYPE EVENT_TYPE AS ENUM ('PARTY', 'BIRTHDAY', 'DINER', 'SNACK');
        EXCEPTION
          WHEN duplicate_object THEN null;
        END $$;
        """.trimIndent(),
      )

      // Create tables using Exposed SchemaUtils
      logger.info("Creating database tables...")
      SchemaUtils.create(EventTable)

      logger.info("Database initialization completed successfully!")
    }
  }
}
