package com.happyrow.core.domain.invite.common.model

import com.happyrow.core.domain.event.common.model.event.EventType
import java.time.Instant
import java.util.UUID

data class InviteValidation(
  val token: String,
  val status: InviteValidationStatus,
  val event: InviteEventSummary?,
  val expiresAt: Instant?,
)

data class InviteEventSummary(
  val identifier: UUID,
  val name: String,
  val eventDate: Instant,
  val location: String,
  val type: EventType,
  val organizerName: String,
  val participantCount: Long,
)
