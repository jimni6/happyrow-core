package com.happyrow.core.infrastructure.technical.config

import com.happyrow.core.infrastructure.contribution.common.driven.ContributionTable
import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import com.happyrow.core.infrastructure.participant.common.driven.ParticipantTable
import com.happyrow.core.infrastructure.resource.common.driven.ResourceTable
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

      // Create RESOURCE_CATEGORY enum if it doesn't exist
      logger.info("Creating RESOURCE_CATEGORY enum...")
      exec(
        """
        DO $$ BEGIN
          CREATE TYPE RESOURCE_CATEGORY AS ENUM ('FOOD', 'DRINK', 'UTENSIL', 'DECORATION', 'OTHER');
        EXCEPTION
          WHEN duplicate_object THEN null;
        END $$;
        """.trimIndent(),
      )

      // Create tables using Exposed SchemaUtils
      logger.info("Creating database tables...")
      SchemaUtils.create(EventTable)
      SchemaUtils.create(ParticipantTable)
      SchemaUtils.create(ResourceTable)
      SchemaUtils.create(ContributionTable)

      // Migrate resource.category column from enum to varchar if needed
      logger.info("Migrating resource.category column type...")
      exec(
        """
        DO $$ BEGIN
          -- Check if column exists and is of type enum
          IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'configuration'
            AND table_name = 'resource'
            AND column_name = 'category'
            AND udt_name = 'resource_category'
          ) THEN
            -- Alter column type from enum to varchar, keeping the data
            ALTER TABLE configuration.resource
            ALTER COLUMN category TYPE VARCHAR(50) USING category::text;

            RAISE NOTICE 'Migrated resource.category from enum to varchar';
          END IF;
        END $$;
        """.trimIndent(),
      )

      logger.info("Database initialization completed successfully!")
    }
  }
}
