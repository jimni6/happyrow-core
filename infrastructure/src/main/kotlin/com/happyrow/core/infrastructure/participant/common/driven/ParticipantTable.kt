package com.happyrow.core.infrastructure.participant.common.driven

import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

private const val STATUS_MAX_LENGTH = 50
private const val EMAIL_MAX_LENGTH = 255

object ParticipantTable : UUIDTable("configuration.participant", "id") {
  val userEmail = varchar("user_email", EMAIL_MAX_LENGTH)
  val eventId = uuid("event_id").references(EventTable.id)
  val status = varchar("status", STATUS_MAX_LENGTH).default("CONFIRMED")
  val joinedAt = timestamp("joined_at")
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")

  init {
    uniqueIndex("uq_participant_user_event", userEmail, eventId)
    index("idx_participant_user", false, userEmail)
    index("idx_participant_event", false, eventId)
  }
}
