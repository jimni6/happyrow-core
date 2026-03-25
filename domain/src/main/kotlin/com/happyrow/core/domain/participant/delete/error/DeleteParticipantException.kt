package com.happyrow.core.domain.participant.delete.error

import java.util.UUID

class DeleteParticipantException(
  val userId: UUID,
  val eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to delete participant $userId for event $eventId", cause)
