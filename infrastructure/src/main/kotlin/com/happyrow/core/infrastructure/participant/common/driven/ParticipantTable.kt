package com.happyrow.core.infrastructure.participant.common.driven

import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

private const val STATUS_MAX_LENGTH = 50
private const val NAME_MAX_LENGTH = 255

object ParticipantTable : UUIDTable("configuration.participant", "id") {
  val userId = uuid("user_id")
  val userName = varchar("user_name", NAME_MAX_LENGTH).nullable().default(null)
  val eventId = uuid("event_id").references(EventTable.id)
  val status = varchar("status", STATUS_MAX_LENGTH).default("CONFIRMED")
  val joinedAt = timestamp("joined_at")
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")

  init {
    uniqueIndex("uq_participant_user_event", userId, eventId)
    index("idx_participant_user", false, userId)
    index("idx_participant_event", false, eventId)
  }
}
