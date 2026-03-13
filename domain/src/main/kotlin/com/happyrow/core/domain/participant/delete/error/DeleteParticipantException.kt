package com.happyrow.core.domain.participant.delete.error

import java.util.UUID

class DeleteParticipantException(
  val userEmail: String,
  val eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to delete participant $userEmail for event $eventId", cause)
