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
      migrateParticipantUserIdToEmail()
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

  private fun org.jetbrains.exposed.sql.Transaction.migrateParticipantUserIdToEmail() {
    logger.info("Migrating participant.user_id to user_email...")
    exec(
      """
      DO $$ BEGIN
        IF EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_schema = 'configuration'
          AND table_name = 'participant'
          AND column_name = 'user_id'
        ) AND NOT EXISTS (
          SELECT 1 FROM information_schema.columns
          WHERE table_schema = 'configuration'
          AND table_name = 'participant'
          AND column_name = 'user_email'
        ) THEN
          ALTER TABLE configuration.participant ADD COLUMN user_email VARCHAR(255);
          ALTER TABLE configuration.contribution DROP CONSTRAINT IF EXISTS contribution_participant_id_fkey;
          DROP INDEX IF EXISTS configuration.uq_participant_user_event;
          DROP INDEX IF EXISTS configuration.idx_participant_user;
          TRUNCATE TABLE configuration.participant CASCADE;
          TRUNCATE TABLE configuration.contribution CASCADE;
          ALTER TABLE configuration.participant DROP COLUMN user_id;
          ALTER TABLE configuration.participant ALTER COLUMN user_email SET NOT NULL;
          CREATE UNIQUE INDEX uq_participant_user_event ON configuration.participant (user_email, event_id);
          CREATE INDEX idx_participant_user ON configuration.participant (user_email);
          ALTER TABLE configuration.contribution
          ADD CONSTRAINT contribution_participant_id_fkey
          FOREIGN KEY (participant_id) REFERENCES configuration.participant(id);
          RAISE NOTICE 'Migrated participant.user_id to user_email';
        END IF;
      END $$;
      """.trimIndent(),
    )
  }
}
