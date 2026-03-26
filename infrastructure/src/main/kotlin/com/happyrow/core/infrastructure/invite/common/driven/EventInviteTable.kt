package com.happyrow.core.infrastructure.invite.common.driven

import com.happyrow.core.domain.invite.common.model.InviteLink
import com.happyrow.core.domain.invite.common.model.InviteStatus
import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.javatime.timestamp

private const val TOKEN_MAX_LENGTH = 64
private const val STATUS_MAX_LENGTH = 20

object EventInviteTable : UUIDTable("configuration.event_invite", "id") {
  val token = varchar("token", TOKEN_MAX_LENGTH)
  val eventId = uuid("event_id").references(EventTable.id)
  val createdBy = uuid("created_by")
  val status = varchar("status", STATUS_MAX_LENGTH).default("ACTIVE")
  val maxUses = integer("max_uses").nullable()
  val currentUses = integer("current_uses").default(0)
  val createdAt = timestamp("created_at")
  val expiresAt = timestamp("expires_at")
}

fun ResultRow.toInviteLink(): InviteLink = InviteLink(
  id = this[EventInviteTable.id].value,
  token = this[EventInviteTable.token],
  eventId = this[EventInviteTable.eventId],
  createdBy = this[EventInviteTable.createdBy],
  status = InviteStatus.valueOf(this[EventInviteTable.status]),
  maxUses = this[EventInviteTable.maxUses],
  currentUses = this[EventInviteTable.currentUses],
  createdAt = this[EventInviteTable.createdAt],
  expiresAt = this[EventInviteTable.expiresAt],
)
