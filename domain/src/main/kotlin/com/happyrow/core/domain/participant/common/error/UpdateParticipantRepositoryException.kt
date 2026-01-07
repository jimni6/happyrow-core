package com.happyrow.core.domain.participant.common.error

import java.util.UUID

class UpdateParticipantRepositoryException(
  val userEmail: String,
  val eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to update participant for user $userEmail and event $eventId", cause)
