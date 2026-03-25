package com.happyrow.core.domain.participant.update.error

import java.util.UUID

class ForbiddenParticipantUpdateException(
  val authenticatedUserId: String,
  val targetUserId: UUID,
  val eventId: UUID,
) : Exception(
  "User $authenticatedUserId is not authorized to update participant $targetUserId for event $eventId",
)
