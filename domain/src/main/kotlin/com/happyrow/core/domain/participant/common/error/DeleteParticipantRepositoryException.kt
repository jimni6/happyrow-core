package com.happyrow.core.domain.participant.common.error

import java.util.UUID

class DeleteParticipantRepositoryException(
  val userEmail: String,
  val eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to delete participant $userEmail for event $eventId", cause)
