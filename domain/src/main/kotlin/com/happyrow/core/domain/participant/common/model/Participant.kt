package com.happyrow.core.domain.participant.common.model

import java.time.Instant
import java.util.UUID

data class Participant(
  val identifier: UUID,
  val userId: UUID,
  val eventId: UUID,
  val status: ParticipantStatus,
  val joinedAt: Instant,
  val createdAt: Instant,
  val updatedAt: Instant,
)
