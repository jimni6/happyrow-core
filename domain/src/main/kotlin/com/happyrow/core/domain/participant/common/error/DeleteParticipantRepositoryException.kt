package com.happyrow.core.domain.participant.common.error

import java.util.UUID

class DeleteParticipantRepositoryException(
  val userId: UUID,
  val eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to delete participant $userId for event $eventId", cause)
