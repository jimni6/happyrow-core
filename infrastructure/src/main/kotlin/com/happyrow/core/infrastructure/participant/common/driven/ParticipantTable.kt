package com.happyrow.core.infrastructure.participant.common.driven

import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

private const val STATUS_MAX_LENGTH = 50

object ParticipantTable : Table("configuration.participant") {
  val id = uuid("id").autoGenerate()
  val userId = uuid("user_id")
  val eventId = uuid("event_id").references(EventTable.id)
  val status = varchar("status", STATUS_MAX_LENGTH).default("CONFIRMED")
  val joinedAt = timestamp("joined_at")
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")

  override val primaryKey = PrimaryKey(id)

  init {
    uniqueIndex("uq_participant_user_event", userId, eventId)
    index("idx_participant_user", false, userId)
    index("idx_participant_event", false, eventId)
  }
}
