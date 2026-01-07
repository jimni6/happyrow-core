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
      createSchema()
      createEnums()
      createTables()
      migrateResourceCategory()
      logger.info("Database initialization completed successfully!")
    }
  }

  private fun org.jetbrains.exposed.sql.Transaction.createSchema() {
    logger.info("Creating configuration schema...")
    exec("CREATE SCHEMA IF NOT EXISTS configuration")
  }

  private fun org.jetbrains.exposed.sql.Transaction.createEnums() {
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
  }

  private fun org.jetbrains.exposed.sql.Transaction.createTables() {
    logger.info("Creating database tables...")
    SchemaUtils.create(EventTable)
    SchemaUtils.create(ParticipantTable)
    SchemaUtils.create(ResourceTable)
    SchemaUtils.create(ContributionTable)
  }

  private fun org.jetbrains.exposed.sql.Transaction.migrateResourceCategory() {
    logger.info("Migrating resource.category column type...")
    exec(
      """
      DO $$ BEGIN
        IF EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_schema = 'configuration'
          AND table_name = 'resource'
          AND column_name = 'category'
          AND udt_name = 'resource_category'
        ) THEN
          ALTER TABLE configuration.resource
          ALTER COLUMN category TYPE VARCHAR(50) USING category::text;
          RAISE NOTICE 'Migrated resource.category from enum to varchar';
        END IF;
      END $$;
      """.trimIndent(),
    )
  }
}
